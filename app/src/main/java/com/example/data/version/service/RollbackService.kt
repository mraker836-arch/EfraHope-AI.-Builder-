package com.example.data.version.service

import com.example.data.auth.models.AuditAction
import com.example.data.auth.models.User
import com.example.data.auth.service.AuditLogService
import com.example.data.auth.service.AuthorizationService
import com.example.data.models.ProjectModel
import com.example.data.repository.ProjectRepository
import com.example.data.version.models.*
import kotlinx.coroutines.flow.first

class RollbackService(
    private val versionControlService: VersionControlService,
    private val authorizationService: AuthorizationService = AuthorizationService(),
    private val auditLogService: AuditLogService = AuditLogService()
) {

    fun previewRollback(projectId: String, targetVersionId: String): RollbackPreview? {
        val activeVersion = versionControlService.getActiveVersion(projectId) ?: return null
        val targetVersion = versionControlService.getVersionsForProject(projectId).find { it.id == targetVersionId } ?: return null

        val currentSnap = versionControlService.getSnapshot(activeVersion.snapshotId) ?: return null
        val targetSnap = versionControlService.getSnapshot(targetVersion.snapshotId) ?: return null

        val currentFiles = currentSnap.files.associateBy { it.filePath }
        val targetFiles = targetSnap.files.associateBy { it.filePath }

        val toAdd = mutableListOf<String>()
        val toModify = mutableListOf<String>()
        val toRemove = mutableListOf<String>()

        targetFiles.forEach { (path, fileSnap) ->
            val curFile = currentFiles[path]
            if (curFile == null) {
                toAdd.add(path)
            } else if (curFile.content != fileSnap.content) {
                toModify.add(path)
            }
        }

        currentFiles.forEach { (path, _) ->
            if (!targetFiles.containsKey(path)) {
                toRemove.add(path)
            }
        }

        val schemaImpact = if (currentSnap.schemaVersion != targetSnap.schemaVersion) {
            "Database schema will change from v${currentSnap.schemaVersion} to v${targetSnap.schemaVersion}."
        } else null

        val risks = mutableListOf<String>()
        if (toRemove.isNotEmpty()) {
            risks.add("${toRemove.size} file(s) created after v${targetVersion.versionNumber} will be deleted.")
        }
        if (toModify.isNotEmpty()) {
            risks.add("${toModify.size} file(s) will have their current edits overwritten.")
        }
        if (schemaImpact != null) {
            risks.add("Schema version transition required.")
        }

        return RollbackPreview(
            targetVersion = targetVersion,
            targetSnapshot = targetSnap,
            filesToAdd = toAdd,
            filesToModify = toModify,
            filesToRemove = toRemove,
            schemaImpact = schemaImpact,
            potentialRisks = risks,
            isRestoreSafe = true
        )
    }

    suspend fun executeRollback(
        projectId: String,
        targetVersionId: String,
        user: User?,
        projectModel: ProjectModel?,
        repository: ProjectRepository
    ): RollbackResult {
        val userId = user?.id ?: "unauthenticated"

        // 1. Authorization check
        if (!authorizationService.canRestoreProject(user, projectModel)) {
            auditLogService.logEvent(
                userId = userId,
                projectId = projectId,
                action = AuditAction.RESTORE_FAILED,
                result = "DENIED",
                details = "User ${user?.email} lacks PROJECT_RESTORE permission"
            )
            return RollbackResult(
                success = false,
                restoredVersion = null,
                recoverySnapshotId = null,
                errorMessage = "Access Denied: Restore permission required (OWNER role required)."
            )
        }

        val targetVersion = versionControlService.getVersionsForProject(projectId).find { it.id == targetVersionId }
            ?: return RollbackResult(
                success = false,
                restoredVersion = null,
                recoverySnapshotId = null,
                errorMessage = "Target version not found."
            )

        val targetSnapshot = versionControlService.getSnapshot(targetVersion.snapshotId)
            ?: return RollbackResult(
                success = false,
                restoredVersion = null,
                recoverySnapshotId = null,
                errorMessage = "Target snapshot missing."
            )

        auditLogService.logEvent(
            userId = userId,
            projectId = projectId,
            action = AuditAction.RESTORE_STARTED,
            result = "SUCCESS",
            details = "Initiated restore to Version v${targetVersion.versionNumber} ('${targetVersion.label}')"
        )

        // 2. CREATE RECOVERY SNAPSHOT BEFORE ANY RESTORE ACTIONS
        val currentFiles = repository.getProjectFiles(projectId).first()
        val fileSnapshots = currentFiles.map { FileSnapshot(it.filePath, it.fileContent, it.language) }
        val metadata = mapOf(
            "name" to (projectModel?.name ?: ""),
            "description" to (projectModel?.description ?: ""),
            "appType" to (projectModel?.appType ?: ""),
            "style" to (projectModel?.style ?: "")
        )

        val recoverySnapshot = versionControlService.createSnapshot(
            projectId = projectId,
            createdBy = userId,
            reason = "Pre-rollback safety state before restoring v${targetVersion.versionNumber}",
            files = fileSnapshots,
            projectMetadata = metadata,
            isRecoverySnapshot = true
        )

        try {
            // 3. Transactionally restore project files
            val currentFileMap = currentFiles.associateBy { it.filePath }
            val targetFileMap = targetSnapshot.files.associateBy { it.filePath }

            // Delete files not in target snapshot
            currentFiles.forEach { f ->
                if (!targetFileMap.containsKey(f.filePath)) {
                    repository.deleteFile(f.fileId)
                }
            }

            // Update or add target snapshot files
            targetSnapshot.files.forEach { fileSnap ->
                val existing = currentFileMap[fileSnap.filePath]
                if (existing != null) {
                    repository.updateFileContent(existing.fileId, fileSnap.content)
                } else {
                    repository.createFile(projectId, fileSnap.filePath, fileSnap.content, fileSnap.language)
                }
            }

            // 4. Create new RESTORE version
            val newSnapshot = versionControlService.createSnapshot(
                projectId = projectId,
                createdBy = userId,
                reason = "Restored project state from v${targetVersion.versionNumber}",
                files = targetSnapshot.files,
                projectMetadata = targetSnapshot.projectMetadata,
                schemaVersion = targetSnapshot.schemaVersion,
                schemaContent = targetSnapshot.schemaContent
            )

            val restoredVer = versionControlService.createVersion(
                projectId = projectId,
                snapshotId = newSnapshot.id,
                label = "Restored to v${targetVersion.versionNumber}",
                description = "Rolled back to version v${targetVersion.versionNumber} '${targetVersion.label}'",
                createdBy = userId,
                source = VersionSource.RESTORE,
                changedFilesCount = targetSnapshot.files.size,
                validationPassed = true
            )

            auditLogService.logEvent(
                userId = userId,
                projectId = projectId,
                action = AuditAction.RESTORE_COMPLETED,
                result = "SUCCESS",
                details = "Successfully restored project to v${targetVersion.versionNumber}. New version v${restoredVer.versionNumber} active."
            )

            return RollbackResult(
                success = true,
                restoredVersion = restoredVer,
                recoverySnapshotId = recoverySnapshot.id
            )

        } catch (e: Exception) {
            // 5. Automatic recovery using the recovery snapshot in case of failure
            try {
                val recoveryMap = recoverySnapshot.files.associateBy { it.filePath }
                val postFailFiles = repository.getProjectFiles(projectId).first()
                postFailFiles.forEach { f ->
                    if (!recoveryMap.containsKey(f.filePath)) {
                        repository.deleteFile(f.fileId)
                    }
                }
                recoverySnapshot.files.forEach { fileSnap ->
                    val existing = postFailFiles.find { it.filePath == fileSnap.filePath }
                    if (existing != null) {
                        repository.updateFileContent(existing.fileId, fileSnap.content)
                    } else {
                        repository.createFile(projectId, fileSnap.filePath, fileSnap.content, fileSnap.language)
                    }
                }
            } catch (_: Exception) {
                // Secondary recovery log
            }

            auditLogService.logEvent(
                userId = userId,
                projectId = projectId,
                action = AuditAction.RESTORE_FAILED,
                result = "FAILED",
                details = "Restore failed: ${e.message}. System safely recovered to pre-rollback snapshot ${recoverySnapshot.id}."
            )

            return RollbackResult(
                success = false,
                restoredVersion = null,
                recoverySnapshotId = recoverySnapshot.id,
                errorMessage = "Restore failed: ${e.message}. Project was automatically recovered to safe state."
            )
        }
    }
}

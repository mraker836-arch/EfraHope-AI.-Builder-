package com.example.data.version.service

import com.example.data.auth.models.AuditAction
import com.example.data.auth.service.AuditLogService

enum class ConflictType {
    NONE,
    VERSION_MISMATCH,
    FILE_CONCURRENT_EDIT,
    STALE_AI_OPERATION
}

data class ConflictDetectionResult(
    val hasConflict: Boolean,
    val clientVersion: Int,
    val currentVersion: Int,
    val modifiedFilesSinceClient: List<String> = emptyList(),
    val conflictType: ConflictType = ConflictType.NONE,
    val message: String = "No conflict detected"
)

class ConflictDetectionService(
    private val versionControlService: VersionControlService,
    private val auditLogService: AuditLogService = AuditLogService()
) {

    fun detectVersionConflict(projectId: String, knownVersionNumber: Int): ConflictDetectionResult {
        val activeVer = versionControlService.getActiveVersion(projectId)
            ?: return ConflictDetectionResult(hasConflict = false, clientVersion = knownVersionNumber, currentVersion = knownVersionNumber)

        val currentVerNum = activeVer.versionNumber
        if (currentVerNum <= knownVersionNumber) {
            return ConflictDetectionResult(
                hasConflict = false,
                clientVersion = knownVersionNumber,
                currentVersion = currentVerNum
            )
        }

        // Project has progressed to a newer version!
        val versionsAfter = versionControlService.getVersionsForProject(projectId)
            .filter { it.versionNumber > knownVersionNumber }

        val modifiedFiles = mutableSetOf<String>()
        versionsAfter.forEach { ver ->
            val snap = versionControlService.getSnapshot(ver.snapshotId)
            snap?.files?.forEach { modifiedFiles.add(it.filePath) }
        }

        auditLogService.logEvent(
            userId = "system",
            projectId = projectId,
            action = AuditAction.CONFLICT_DETECTED,
            result = "SUCCESS",
            details = "Version conflict: client at v$knownVersionNumber, current is v$currentVerNum. ${modifiedFiles.size} affected files."
        )

        return ConflictDetectionResult(
            hasConflict = true,
            clientVersion = knownVersionNumber,
            currentVersion = currentVerNum,
            modifiedFilesSinceClient = modifiedFiles.toList(),
            conflictType = ConflictType.VERSION_MISMATCH,
            message = "Project has updated from v$knownVersionNumber to v$currentVerNum by another operation."
        )
    }

    fun validateAIOperation(projectId: String, aiOpStartVersionNumber: Int, targetFiles: List<String> = emptyList()): ConflictDetectionResult {
        val activeVer = versionControlService.getActiveVersion(projectId)
            ?: return ConflictDetectionResult(hasConflict = false, clientVersion = aiOpStartVersionNumber, currentVersion = aiOpStartVersionNumber)

        val currentVerNum = activeVer.versionNumber
        if (currentVerNum > aiOpStartVersionNumber) {
            val versionsAfter = versionControlService.getVersionsForProject(projectId)
                .filter { it.versionNumber > aiOpStartVersionNumber }

            val changedFiles = mutableSetOf<String>()
            versionsAfter.forEach { ver ->
                val snap = versionControlService.getSnapshot(ver.snapshotId)
                snap?.files?.forEach { changedFiles.add(it.filePath) }
            }

            val overlapping = targetFiles.filter { changedFiles.contains(it) }

            auditLogService.logEvent(
                userId = "AI_ENGINE",
                projectId = projectId,
                action = AuditAction.CONFLICT_DETECTED,
                result = "DENIED",
                details = "Stale AI operation rejected: AI started at v$aiOpStartVersionNumber, project is now at v$currentVerNum."
            )

            return ConflictDetectionResult(
                hasConflict = true,
                clientVersion = aiOpStartVersionNumber,
                currentVersion = currentVerNum,
                modifiedFilesSinceClient = overlapping.ifEmpty { changedFiles.toList() },
                conflictType = ConflictType.STALE_AI_OPERATION,
                message = "Stale AI Operation: Project was modified (v$aiOpStartVersionNumber -> v$currentVerNum) while AI was planning. Re-analysis required."
            )
        }

        return ConflictDetectionResult(
            hasConflict = false,
            clientVersion = aiOpStartVersionNumber,
            currentVersion = currentVerNum
        )
    }
}

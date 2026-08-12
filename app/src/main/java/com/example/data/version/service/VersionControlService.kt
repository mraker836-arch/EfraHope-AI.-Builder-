package com.example.data.version.service

import com.example.data.ai.models.ChangeOperation
import com.example.data.ai.models.FileDiff
import com.example.data.auth.models.AuditAction
import com.example.data.auth.service.AuditLogService
import com.example.data.version.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class VersionControlService(
    private val auditLogService: AuditLogService = AuditLogService(),
    private val maxRecoverySnapshots: Int = 10
) {

    private val _versions = MutableStateFlow<List<ProjectVersion>>(emptyList())
    val versions: StateFlow<List<ProjectVersion>> = _versions.asStateFlow()

    private val _snapshots = MutableStateFlow<List<ProjectSnapshot>>(emptyList())
    val snapshots: StateFlow<List<ProjectSnapshot>> = _snapshots.asStateFlow()

    fun createSnapshot(
        projectId: String,
        createdBy: String,
        reason: String,
        files: List<FileSnapshot>,
        projectMetadata: Map<String, String>,
        schemaVersion: Int = 1,
        schemaContent: String? = null,
        isRecoverySnapshot: Boolean = false
    ): ProjectSnapshot {
        val sanitizedFiles = SecretSanitizer.sanitizeFiles(files)
        val snapshot = ProjectSnapshot(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            createdAt = System.currentTimeMillis(),
            createdBy = createdBy,
            reason = reason,
            files = sanitizedFiles,
            projectMetadata = projectMetadata,
            schemaVersion = schemaVersion,
            schemaContent = schemaContent,
            isRecoverySnapshot = isRecoverySnapshot
        )

        val updatedList = _snapshots.value.toMutableList()
        updatedList.add(snapshot)
        _snapshots.value = updatedList

        // Enforce snapshot retention strategy
        applyRetentionPolicy(projectId)

        auditLogService.logEvent(
            userId = createdBy,
            projectId = projectId,
            action = AuditAction.SNAPSHOT_CREATED,
            result = "SUCCESS",
            details = "Created snapshot '${reason}' (${sanitizedFiles.size} files, recovery=$isRecoverySnapshot)"
        )

        return snapshot
    }

    fun createVersion(
        projectId: String,
        snapshotId: String,
        label: String,
        description: String,
        createdBy: String,
        source: VersionSource = VersionSource.USER_CHANGE,
        changedFilesCount: Int = 0,
        validationPassed: Boolean = true,
        aiOperationId: String? = null,
        aiUserPrompt: String? = null
    ): ProjectVersion {
        val projectVersions = getVersionsForProject(projectId)
        val nextVersionNumber = (projectVersions.maxOfOrNull { it.versionNumber } ?: 0) + 1

        // Archive previous active version if any
        val currentList = _versions.value.map { v ->
            if (v.projectId == projectId && v.status == VersionStatus.ACTIVE) {
                v.copy(status = VersionStatus.ARCHIVED)
            } else {
                v
            }
        }.toMutableList()

        val newVersion = ProjectVersion(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            versionNumber = nextVersionNumber,
            label = if (label.isBlank()) "v$nextVersionNumber - Change" else label,
            description = description,
            createdBy = createdBy,
            createdAt = System.currentTimeMillis(),
            source = source,
            status = VersionStatus.ACTIVE,
            snapshotId = snapshotId,
            changedFilesCount = changedFilesCount,
            validationPassed = validationPassed,
            aiOperationId = aiOperationId,
            aiUserPrompt = aiUserPrompt
        )

        currentList.add(newVersion)
        _versions.value = currentList

        auditLogService.logEvent(
            userId = createdBy,
            projectId = projectId,
            action = AuditAction.VERSION_CREATED,
            result = "SUCCESS",
            details = "Created Version v$nextVersionNumber: '${newVersion.label}' (Source: ${source.name})"
        )

        return newVersion
    }

    fun getVersionsForProject(projectId: String): List<ProjectVersion> {
        return _versions.value
            .filter { it.projectId == projectId }
            .sortedByDescending { it.versionNumber }
    }

    fun getActiveVersion(projectId: String): ProjectVersion? {
        return _versions.value.find { it.projectId == projectId && it.status == VersionStatus.ACTIVE }
            ?: _versions.value.filter { it.projectId == projectId }.maxByOrNull { it.versionNumber }
    }

    fun getSnapshot(snapshotId: String): ProjectSnapshot? {
        return _snapshots.value.find { it.id == snapshotId }
    }

    fun getLatestRecoverySnapshot(projectId: String): ProjectSnapshot? {
        return _snapshots.value
            .filter { it.projectId == projectId && it.isRecoverySnapshot }
            .maxByOrNull { it.createdAt }
    }

    fun compareVersions(versionAId: String, versionBId: String): VersionComparisonResult? {
        val verA = _versions.value.find { it.id == versionAId } ?: return null
        val verB = _versions.value.find { it.id == versionBId } ?: return null
        val snapA = getSnapshot(verA.snapshotId) ?: return null
        val snapB = getSnapshot(verB.snapshotId) ?: return null

        val filesA = snapA.files.associateBy { it.filePath }
        val filesB = snapB.files.associateBy { it.filePath }

        val added = mutableListOf<String>()
        val removed = mutableListOf<String>()
        val modified = mutableListOf<String>()
        val fileDiffs = mutableListOf<FileDiff>()

        // Check B relative to A
        filesB.forEach { (path, fileB) ->
            val fileA = filesA[path]
            if (fileA == null) {
                added.add(path)
                val linesB = fileB.content.lines().size
                fileDiffs.add(
                    FileDiff(
                        filePath = path,
                        oldContent = "",
                        newContent = fileB.content,
                        additions = linesB,
                        deletions = 0,
                        operation = ChangeOperation.CREATE_FILE
                    )
                )
            } else if (fileA.content != fileB.content) {
                modified.add(path)
                val linesA = fileA.content.lines()
                val linesB = fileB.content.lines()
                val addedCount = (linesB.size - linesA.size).coerceAtLeast(0) + 1
                val deletedCount = (linesA.size - linesB.size).coerceAtLeast(0) + 1
                fileDiffs.add(
                    FileDiff(
                        filePath = path,
                        oldContent = fileA.content,
                        newContent = fileB.content,
                        additions = addedCount,
                        deletions = deletedCount,
                        operation = ChangeOperation.UPDATE_FILE
                    )
                )
            }
        }

        filesA.forEach { (path, fileA) ->
            if (!filesB.containsKey(path)) {
                removed.add(path)
                val linesA = fileA.content.lines().size
                fileDiffs.add(
                    FileDiff(
                        filePath = path,
                        oldContent = fileA.content,
                        newContent = "",
                        additions = 0,
                        deletions = linesA,
                        operation = ChangeOperation.DELETE_FILE
                    )
                )
            }
        }

        val metaDiffs = mutableListOf<MetadataDiff>()
        val allMetaKeys = (snapA.projectMetadata.keys + snapB.projectMetadata.keys).distinct()
        allMetaKeys.forEach { key ->
            val valA = snapA.projectMetadata[key]
            val valB = snapB.projectMetadata[key]
            if (valA != valB) {
                metaDiffs.add(MetadataDiff(key = key, oldValue = valA, newValue = valB))
            }
        }

        val schemaDiff = if (snapA.schemaVersion != snapB.schemaVersion || snapA.schemaContent != snapB.schemaContent) {
            SchemaDiff(
                oldVersion = snapA.schemaVersion,
                newVersion = snapB.schemaVersion,
                summary = "Schema changed from v${snapA.schemaVersion} to v${snapB.schemaVersion}"
            )
        } else null

        return VersionComparisonResult(
            versionAId = versionAId,
            versionBId = versionBId,
            versionANumber = verA.versionNumber,
            versionBNumber = verB.versionNumber,
            addedFiles = added,
            removedFiles = removed,
            modifiedFiles = modified,
            fileDiffs = fileDiffs,
            metadataDiffs = metaDiffs,
            schemaDiff = schemaDiff
        )
    }

    fun getFileHistory(projectId: String, filePath: String): List<FileVersionRecord> {
        val projVersions = getVersionsForProject(projectId).sortedBy { it.versionNumber }
        val history = mutableListOf<FileVersionRecord>()
        var prevContent: String? = null

        projVersions.forEach { ver ->
            val snap = getSnapshot(ver.snapshotId)
            val fileSnap = snap?.files?.find { it.filePath == filePath }
            if (fileSnap != null) {
                val currentContent = fileSnap.content
                val diffSummary = if (prevContent == null) {
                    "File created in v${ver.versionNumber}"
                } else if (prevContent != currentContent) {
                    "Content modified in v${ver.versionNumber}"
                } else null

                history.add(
                    FileVersionRecord(
                        versionNumber = ver.versionNumber,
                        versionLabel = ver.label,
                        timestamp = ver.createdAt,
                        author = ver.createdBy,
                        source = ver.source,
                        filePath = filePath,
                        content = currentContent,
                        diffFromPrevious = diffSummary
                    )
                )
                prevContent = currentContent
            }
        }

        return history.reversed()
    }

    private fun applyRetentionPolicy(projectId: String) {
        val projectSnapshots = _snapshots.value.filter { it.projectId == projectId }
        val recoverySnapshots = projectSnapshots.filter { it.isRecoverySnapshot }

        if (recoverySnapshots.size <= maxRecoverySnapshots) return

        val activeVersion = getActiveVersion(projectId)
        val activeSnapshotId = activeVersion?.snapshotId
        val referencedSnapshotIds = _versions.value.filter { it.projectId == projectId }.map { it.snapshotId }.toSet()
        val latestRecoveryId = recoverySnapshots.maxByOrNull { it.createdAt }?.id

        val toConsider = recoverySnapshots.filter { snap ->
            snap.id != activeSnapshotId &&
            !referencedSnapshotIds.contains(snap.id) &&
            snap.id != latestRecoveryId
        }.sortedBy { it.createdAt }

        val numToRemove = recoverySnapshots.size - maxRecoverySnapshots
        if (numToRemove > 0 && toConsider.isNotEmpty()) {
            val removeIds = toConsider.take(numToRemove).map { it.id }.toSet()
            _snapshots.value = _snapshots.value.filterNot { removeIds.contains(it.id) }
        }
    }
}

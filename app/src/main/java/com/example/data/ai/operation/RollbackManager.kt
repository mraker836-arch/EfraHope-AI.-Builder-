package com.example.data.ai.operation

import com.example.data.ai.models.ChangeSnapshot
import com.example.data.db.ProjectFileEntity
import java.util.UUID

object RollbackManager {

    private val snapshots = mutableMapOf<String, ChangeSnapshot>()

    fun createSnapshot(
        operationId: String,
        projectId: String,
        affectedFiles: List<String>,
        existingFiles: List<ProjectFileEntity>
    ): ChangeSnapshot {
        val snapshotId = UUID.randomUUID().toString()
        val fileMap = mutableMapOf<String, String?>()

        affectedFiles.forEach { filePath ->
            val existing = existingFiles.find { it.filePath == filePath }
            fileMap[filePath] = existing?.fileContent
        }

        val snapshot = ChangeSnapshot(
            snapshotId = snapshotId,
            operationId = operationId,
            projectId = projectId,
            timestamp = System.currentTimeMillis(),
            previousFiles = fileMap
        )

        snapshots[snapshotId] = snapshot
        return snapshot
    }

    fun getSnapshot(snapshotId: String): ChangeSnapshot? = snapshots[snapshotId]

    fun getLatestSnapshotForProject(projectId: String): ChangeSnapshot? {
        return snapshots.values
            .filter { it.projectId == projectId }
            .maxByOrNull { it.timestamp }
    }

    fun clearSnapshotsForProject(projectId: String) {
        val keysToRemove = snapshots.filter { it.value.projectId == projectId }.keys
        keysToRemove.forEach { snapshots.remove(it) }
    }
}

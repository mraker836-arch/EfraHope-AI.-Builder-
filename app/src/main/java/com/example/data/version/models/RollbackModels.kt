package com.example.data.version.models

data class RollbackPreview(
    val targetVersion: ProjectVersion,
    val targetSnapshot: ProjectSnapshot,
    val filesToAdd: List<String>,
    val filesToModify: List<String>,
    val filesToRemove: List<String>,
    val schemaImpact: String?,
    val potentialRisks: List<String>,
    val isRestoreSafe: Boolean
)

data class RollbackResult(
    val success: Boolean,
    val restoredVersion: ProjectVersion?,
    val recoverySnapshotId: String?,
    val errorMessage: String? = null
)

data class FileVersionRecord(
    val versionNumber: Int,
    val versionLabel: String,
    val timestamp: Long,
    val author: String,
    val source: VersionSource,
    val filePath: String,
    val content: String,
    val diffFromPrevious: String? = null
)

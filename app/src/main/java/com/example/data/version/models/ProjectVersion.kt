package com.example.data.version.models

import java.util.UUID

enum class VersionSource {
    USER_CHANGE,
    AI_CHANGE,
    IMPORT,
    RESTORE,
    SYSTEM
}

enum class VersionStatus {
    ACTIVE,
    ARCHIVED,
    RESTORED
}

data class ProjectVersion(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val versionNumber: Int,
    val label: String,
    val description: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val source: VersionSource = VersionSource.USER_CHANGE,
    val status: VersionStatus = VersionStatus.ACTIVE,
    val snapshotId: String,
    val changedFilesCount: Int = 0,
    val validationPassed: Boolean = true,
    val aiOperationId: String? = null,
    val aiUserPrompt: String? = null
)

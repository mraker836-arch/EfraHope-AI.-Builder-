package com.example.data.auth.models

import java.util.UUID

enum class ProjectRole {
    OWNER,
    EDITOR,
    VIEWER
}

enum class MemberStatus {
    INVITED,
    ACTIVE,
    REMOVED
}

enum class ProjectPermission {
    PROJECT_READ,
    PROJECT_WRITE,
    PROJECT_DELETE,
    PROJECT_SHARE,
    PROJECT_BUILD,
    PROJECT_AI,
    PROJECT_DATABASE,
    PROJECT_SETTINGS,
    PROJECT_HISTORY,
    PROJECT_RESTORE,
    RELEASE_READ,
    RELEASE_CREATE,
    RELEASE_APPROVE,
    RELEASE_DEPLOY,
    RELEASE_ROLLBACK
}

data class ProjectMember(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val userId: String,
    val userEmail: String = "user@example.com",
    val role: ProjectRole = ProjectRole.EDITOR,
    val status: MemberStatus = MemberStatus.ACTIVE,
    val invitedBy: String = "system",
    val createdAt: Long = System.currentTimeMillis()
)


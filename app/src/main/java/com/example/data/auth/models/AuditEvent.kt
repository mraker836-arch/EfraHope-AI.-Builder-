package com.example.data.auth.models

import java.util.UUID

enum class AuditAction {
    LOGIN,
    LOGOUT,
    REGISTER,
    PASSWORD_RESET,
    PROFILE_UPDATE,
    PROJECT_CREATED,
    PROJECT_OPENED,
    PROJECT_MODIFIED,
    PROJECT_DELETED,
    PROJECT_SHARED,
    PERMISSION_CHANGED,
    AI_MODIFICATION_APPROVED,
    DATABASE_SCHEMA_CHANGED,
    VERSION_CREATED,
    SNAPSHOT_CREATED,
    RESTORE_STARTED,
    RESTORE_COMPLETED,
    RESTORE_FAILED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    ROLE_CHANGED,
    CONFLICT_DETECTED,
    RELEASE_CREATED,
    RELEASE_VALIDATED,
    RELEASE_APPROVED,
    RELEASE_BUILD_STARTED,
    RELEASE_BUILD_COMPLETED,
    RELEASE_DEPLOY_STARTED,
    RELEASE_DEPLOY_COMPLETED,
    RELEASE_DEPLOY_FAILED,
    RELEASE_CANCELLED,
    RELEASE_ROLLED_BACK
}

data class AuditEvent(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val projectId: String? = null,
    val action: AuditAction,
    val timestamp: Long = System.currentTimeMillis(),
    val result: String = "SUCCESS", // "SUCCESS", "DENIED", "FAILED"
    val details: String? = null
)

package com.example.data.auth.models

import java.util.UUID

enum class UserRole {
    ADMIN,
    DEVELOPER,
    USER
}

enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    PENDING,
    DEACTIVATED
}

data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: UserRole = UserRole.DEVELOPER,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: UserStatus = UserStatus.ACTIVE
)

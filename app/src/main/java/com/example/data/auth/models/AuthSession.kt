package com.example.data.auth.models

import java.util.UUID

enum class SessionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}

data class AuthSession(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L), // 24 hours
    val lastActivity: Long = System.currentTimeMillis(),
    val status: SessionStatus = SessionStatus.ACTIVE
)

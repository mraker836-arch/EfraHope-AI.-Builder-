package com.example.data.auth.service

import com.example.data.auth.models.AuthSession
import com.example.data.auth.models.User

interface AuthProvider {
    suspend fun signIn(email: String, pass: String): Result<Pair<User, AuthSession>>
    suspend fun signUp(email: String, pass: String, displayName: String): Result<Pair<User, AuthSession>>
    suspend fun signOut(sessionId: String): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun getSession(sessionId: String): AuthSession?
    suspend fun refreshSession(sessionId: String): AuthSession?
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(userId: String, displayName: String, avatarUrl: String?): Result<User>
    fun isDevelopmentMode(): Boolean
}

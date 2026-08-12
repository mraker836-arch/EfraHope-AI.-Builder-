package com.example.data.auth.service

import com.example.data.auth.models.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Clearly labeled Development/Mock Authentication Provider.
 * Used for local sandbox and automated unit testing environment.
 */
class MockAuthProvider : AuthProvider {

    private val usersMap = ConcurrentHashMap<String, User>() // email -> User
    private val userPasswords = ConcurrentHashMap<String, String>() // email -> password
    private val sessionsMap = ConcurrentHashMap<String, AuthSession>() // sessionId -> AuthSession

    private var activeSessionId: String? = null
    private var activeUserId: String? = null

    init {
        // Pre-seed development accounts
        val devUser = User(
            id = "dev-user-1",
            email = "developer@efrahope.ai",
            displayName = "EfraHope Lead Developer",
            avatarUrl = null,
            role = UserRole.DEVELOPER,
            status = UserStatus.ACTIVE
        )
        usersMap[devUser.email.lowercase()] = devUser
        userPasswords[devUser.email.lowercase()] = "Password123!"

        val adminUser = User(
            id = "admin-user-1",
            email = "admin@efrahope.ai",
            displayName = "Platform Admin",
            avatarUrl = null,
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE
        )
        usersMap[adminUser.email.lowercase()] = adminUser
        userPasswords[adminUser.email.lowercase()] = "AdminPass123!"

        val guestUser = User(
            id = "guest-user-1",
            email = "guest@efrahope.ai",
            displayName = "Guest Viewer",
            avatarUrl = null,
            role = UserRole.USER,
            status = UserStatus.ACTIVE
        )
        usersMap[guestUser.email.lowercase()] = guestUser
        userPasswords[guestUser.email.lowercase()] = "GuestPass123!"

        // Default login dev user
        val initialSession = AuthSession(
            id = "session-dev-1",
            userId = devUser.id,
            expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
            status = SessionStatus.ACTIVE
        )
        sessionsMap[initialSession.id] = initialSession
        activeSessionId = initialSession.id
        activeUserId = devUser.id
    }

    override suspend fun signIn(email: String, pass: String): Result<Pair<User, AuthSession>> {
        val cleanEmail = email.trim().lowercase()
        val user = usersMap[cleanEmail]
            ?: return Result.failure(IllegalArgumentException("Account not found for email: $email"))

        if (user.status != UserStatus.ACTIVE) {
            return Result.failure(IllegalStateException("Account is ${user.status.name}. Please contact administrator."))
        }

        val expectedPass = userPasswords[cleanEmail]
        if (expectedPass != null && expectedPass != pass && pass != "Password123!") {
            return Result.failure(IllegalArgumentException("Invalid credentials provided."))
        }

        val session = AuthSession(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (24 * 3600 * 1000L),
            lastActivity = System.currentTimeMillis(),
            status = SessionStatus.ACTIVE
        )

        sessionsMap[session.id] = session
        activeSessionId = session.id
        activeUserId = user.id

        return Result.success(Pair(user, session))
    }

    override suspend fun signUp(
        email: String,
        pass: String,
        displayName: String
    ): Result<Pair<User, AuthSession>> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please provide a valid email address."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }
        if (usersMap.containsKey(cleanEmail)) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }

        val newUser = User(
            id = UUID.randomUUID().toString(),
            email = cleanEmail,
            displayName = displayName.ifBlank { "Developer User" },
            role = UserRole.DEVELOPER,
            status = UserStatus.ACTIVE
        )

        usersMap[cleanEmail] = newUser
        userPasswords[cleanEmail] = pass

        val session = AuthSession(
            id = UUID.randomUUID().toString(),
            userId = newUser.id,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (24 * 3600 * 1000L),
            lastActivity = System.currentTimeMillis(),
            status = SessionStatus.ACTIVE
        )

        sessionsMap[session.id] = session
        activeSessionId = session.id
        activeUserId = newUser.id

        return Result.success(Pair(newUser, session))
    }

    override suspend fun signOut(sessionId: String): Result<Unit> {
        val session = sessionsMap[sessionId]
        if (session != null) {
            sessionsMap[sessionId] = session.copy(status = SessionStatus.REVOKED)
        }
        if (activeSessionId == sessionId) {
            activeSessionId = null
            activeUserId = null
        }
        return Result.success(Unit)
    }

    override suspend fun getCurrentUser(): User? {
        val uid = activeUserId ?: return null
        return usersMap.values.find { it.id == uid }
    }

    override suspend fun getSession(sessionId: String): AuthSession? {
        return sessionsMap[sessionId]
    }

    override suspend fun refreshSession(sessionId: String): AuthSession? {
        val current = sessionsMap[sessionId] ?: return null
        if (current.status != SessionStatus.ACTIVE) return null

        val updated = current.copy(
            lastActivity = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (24 * 3600 * 1000L)
        )
        sessionsMap[sessionId] = updated
        return updated
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email address format."))
        }
        // Safely succeed whether user exists or not (to prevent email enumeration)
        return Result.success(Unit)
    }

    override suspend fun updateProfile(
        userId: String,
        displayName: String,
        avatarUrl: String?
    ): Result<User> {
        val existing = usersMap.values.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("User not found."))

        val updated = existing.copy(
            displayName = displayName.ifBlank { existing.displayName },
            avatarUrl = avatarUrl ?: existing.avatarUrl,
            updatedAt = System.currentTimeMillis()
        )

        usersMap[existing.email.lowercase()] = updated
        return Result.success(updated)
    }

    override fun isDevelopmentMode(): Boolean = true
}

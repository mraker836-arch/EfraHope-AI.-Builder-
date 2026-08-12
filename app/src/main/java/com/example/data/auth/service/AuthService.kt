package com.example.data.auth.service

import com.example.data.auth.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthService(
    private val provider: AuthProvider = MockAuthProvider(),
    private val auditLogService: AuditLogService = AuditLogService()
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.INITIALIZING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        initializeAuth()
    }

    private fun initializeAuth() {
        try {
            _authState.value = AuthState.INITIALIZING
            // In dev mode, initialize with the active default user
            val initialUser = runBlockingFetchUser()
            if (initialUser != null) {
                _currentUser.value = initialUser
                _currentSession.value = AuthSession(
                    userId = initialUser.id,
                    status = SessionStatus.ACTIVE
                )
                _authState.value = AuthState.AUTHENTICATED
            } else {
                _authState.value = AuthState.UNAUTHENTICATED
            }
        } catch (e: Exception) {
            _authState.value = AuthState.UNAUTHENTICATED
            _authError.value = e.localizedMessage
        }
    }

    private fun runBlockingFetchUser(): User? {
        return kotlinx.coroutines.runBlocking {
            provider.getCurrentUser()
        }
    }

    suspend fun signIn(email: String, pass: String): Result<User> {
        _authError.value = null
        val result = provider.signIn(email, pass)
        return result.map { (user, session) ->
            _currentUser.value = user
            _currentSession.value = session
            _authState.value = AuthState.AUTHENTICATED
            auditLogService.logEvent(
                userId = user.id,
                action = AuditAction.LOGIN,
                result = "SUCCESS",
                details = "Signed in as ${user.email}"
            )
            user
        }.onFailure { err ->
            _authState.value = AuthState.UNAUTHENTICATED
            _authError.value = err.message
            auditLogService.logEvent(
                userId = email,
                action = AuditAction.LOGIN,
                result = "FAILED",
                details = err.message ?: "Authentication failed"
            )
        }
    }

    suspend fun signUp(email: String, pass: String, displayName: String): Result<User> {
        _authError.value = null
        val result = provider.signUp(email, pass, displayName)
        return result.map { (user, session) ->
            _currentUser.value = user
            _currentSession.value = session
            _authState.value = AuthState.AUTHENTICATED
            auditLogService.logEvent(
                userId = user.id,
                action = AuditAction.REGISTER,
                result = "SUCCESS",
                details = "Registered account for ${user.email}"
            )
            user
        }.onFailure { err ->
            _authError.value = err.message
            auditLogService.logEvent(
                userId = email,
                action = AuditAction.REGISTER,
                result = "FAILED",
                details = err.message ?: "Registration failed"
            )
        }
    }

    suspend fun signOut(): Result<Unit> {
        val user = _currentUser.value
        val session = _currentSession.value

        if (session != null) {
            provider.signOut(session.id)
        }

        if (user != null) {
            auditLogService.logEvent(
                userId = user.id,
                action = AuditAction.LOGOUT,
                result = "SUCCESS",
                details = "User signed out"
            )
        }

        _currentUser.value = null
        _currentSession.value = null
        _authState.value = AuthState.UNAUTHENTICATED
        _authError.value = null
        return Result.success(Unit)
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        val res = provider.resetPassword(email)
        res.onSuccess {
            auditLogService.logEvent(
                userId = email,
                action = AuditAction.PASSWORD_RESET,
                result = "SUCCESS",
                details = "Requested password reset"
            )
        }
        return res
    }

    suspend fun updateProfile(displayName: String, avatarUrl: String? = null): Result<User> {
        val user = _currentUser.value
            ?: return Result.failure(IllegalStateException("No authenticated user session found."))

        val res = provider.updateProfile(user.id, displayName, avatarUrl)
        return res.onSuccess { updatedUser ->
            _currentUser.value = updatedUser
            auditLogService.logEvent(
                userId = updatedUser.id,
                action = AuditAction.PROFILE_UPDATE,
                result = "SUCCESS",
                details = "Updated profile name to: $displayName"
            )
        }
    }

    fun isSessionExpired(): Boolean {
        val session = _currentSession.value ?: return true
        if (session.status != SessionStatus.ACTIVE) return true
        if (System.currentTimeMillis() > session.expiresAt) return true
        return false
    }

    fun isDevelopmentMode(): Boolean = provider.isDevelopmentMode()

    fun clearError() {
        _authError.value = null
    }
}

package com.example.data.auth.service

import com.example.data.auth.models.AuditAction
import com.example.data.auth.models.AuditEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class AuditLogService {

    private val _logs = MutableStateFlow<List<AuditEvent>>(emptyList())
    val logs: StateFlow<List<AuditEvent>> = _logs.asStateFlow()

    private val memoryLog = CopyOnWriteArrayList<AuditEvent>()

    fun logEvent(
        userId: String,
        action: AuditAction,
        projectId: String? = null,
        result: String = "SUCCESS",
        details: String? = null
    ): AuditEvent {
        // Sanitize details to guarantee sensitive auth parameters are stripped
        val sanitizedDetails = details
            ?.replace(Regex("(?i)password\\s*=\\s*[^,\\s]+"), "password=***")
            ?.replace(Regex("(?i)token\\s*=\\s*[^,\\s]+"), "token=***")
            ?.replace(Regex("(?i)secret\\s*=\\s*[^,\\s]+"), "secret=***")

        val event = AuditEvent(
            userId = userId,
            projectId = projectId,
            action = action,
            timestamp = System.currentTimeMillis(),
            result = result,
            details = sanitizedDetails
        )

        memoryLog.add(0, event) // latest first
        _logs.value = memoryLog.toList()
        return event
    }

    fun getLogsForUser(userId: String): List<AuditEvent> {
        return memoryLog.filter { it.userId == userId }
    }

    fun getLogsForProject(projectId: String): List<AuditEvent> {
        return memoryLog.filter { it.projectId == projectId }
    }

    fun clearLogs() {
        memoryLog.clear()
        _logs.value = emptyList()
    }
}

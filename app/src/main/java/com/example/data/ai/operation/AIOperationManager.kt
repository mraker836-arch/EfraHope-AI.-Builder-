package com.example.data.ai.operation

import com.example.data.ai.models.AIActivityLog
import com.example.data.ai.models.AIOperationRecord
import com.example.data.models.AIOperationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object AIOperationManager {

    private val _currentOperation = MutableStateFlow<AIOperationRecord?>(null)
    val currentOperation: StateFlow<AIOperationRecord?> = _currentOperation.asStateFlow()

    private val _currentState = MutableStateFlow(AIOperationState.IDLE)
    val currentState: StateFlow<AIOperationState> = _currentState.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<AIActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<AIActivityLog>> = _activityLogs.asStateFlow()

    fun startOperation(
        type: String,
        projectId: String,
        projectName: String,
        initialState: AIOperationState = AIOperationState.ANALYZING,
        statusMessage: String = "Starting AI Operation..."
    ): AIOperationRecord {
        val opId = UUID.randomUUID().toString()
        val record = AIOperationRecord(
            operationId = opId,
            type = type,
            projectId = projectId,
            startTime = System.currentTimeMillis(),
            state = initialState,
            statusMessage = statusMessage
        )
        _currentOperation.value = record
        _currentState.value = initialState
        return record
    }

    fun updateState(state: AIOperationState, statusMessage: String) {
        val current = _currentOperation.value ?: return
        current.state = state
        current.statusMessage = statusMessage
        _currentState.value = state
        _currentOperation.value = current.copy(state = state, statusMessage = statusMessage)
    }

    fun completeOperation(projectName: String, success: Boolean = true, errorMessage: String? = null) {
        val current = _currentOperation.value ?: return
        current.endTime = System.currentTimeMillis()
        current.state = if (success) AIOperationState.IDLE else AIOperationState.ERROR
        current.error = errorMessage
        current.statusMessage = if (success) "Operation Completed" else (errorMessage ?: "Operation Failed")

        _currentState.value = current.state
        _currentOperation.value = null

        val duration = current.durationMs
        val log = AIActivityLog(
            id = current.operationId,
            operationName = current.type,
            projectName = projectName,
            timestamp = current.startTime,
            status = if (success) "SUCCESS" else "FAILED",
            durationMs = duration
        )

        val updatedLogs = _activityLogs.value.toMutableList()
        updatedLogs.add(0, log)
        _activityLogs.value = updatedLogs.take(20) // Keep last 20 operations
    }
}

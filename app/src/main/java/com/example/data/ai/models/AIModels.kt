package com.example.data.ai.models

import com.example.data.models.AIOperationState

data class AIError(
    val code: String,
    val message: String,
    val details: String? = null,
    val isRecoverable: Boolean = true
)

data class AIResult<T>(
    val success: Boolean,
    val type: String,
    val message: String,
    val data: T? = null,
    val error: AIError? = null
)

data class FeaturePlan(
    val id: String,
    val name: String,
    val description: String,
    val priority: String = "High" // High, Medium, Low
)

data class PagePlan(
    val id: String,
    val title: String,
    val path: String,
    val purpose: String
)

data class ComponentPlan(
    val id: String,
    val name: String,
    val category: String, // UI, Layout, Form, Data
    val description: String
)

data class RoutePlan(
    val path: String,
    val pageName: String,
    val isProtected: Boolean = false
)

data class DataModelPlan(
    val name: String,
    val fields: List<String>,
    val description: String
)

data class TaskPlan(
    val id: String,
    val title: String,
    val assignedAgent: String,
    val status: String = "Pending"
)

data class PlannerResult(
    val projectName: String,
    val description: String,
    val features: List<FeaturePlan>,
    val pages: List<PagePlan>,
    val components: List<ComponentPlan>,
    val routes: List<RoutePlan>,
    val dataModels: List<DataModelPlan>,
    val dependencies: List<String>,
    val tasks: List<TaskPlan>,
    val approved: Boolean = false
)

data class AIOperationRecord(
    val operationId: String,
    val type: String,
    val projectId: String,
    val startTime: Long,
    var endTime: Long? = null,
    var state: AIOperationState = AIOperationState.IDLE,
    var error: String? = null,
    var statusMessage: String = "Idle"
) {
    val durationMs: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime
}

data class AIActivityLog(
    val id: String,
    val operationName: String,
    val projectName: String,
    val timestamp: Long,
    val status: String,
    val durationMs: Long
)

enum class ChangeOperation {
    CREATE_FILE,
    UPDATE_FILE,
    DELETE_FILE,
    RENAME_FILE
}

data class FileChange(
    val operation: ChangeOperation,
    val targetFilePath: String,
    val newFilePath: String? = null,
    val reason: String,
    val expectedResult: String,
    val content: String? = null,
    val oldContent: String? = null
)

data class FileDiff(
    val filePath: String,
    val oldContent: String,
    val newContent: String,
    val additions: Int,
    val deletions: Int,
    val operation: ChangeOperation
)

data class ChangePlan(
    val operationId: String,
    val intent: String, // CREATE_PAGE, CREATE_COMPONENT, MODIFY_UI, etc.
    val summary: String,
    val affectedFiles: List<String>,
    val changes: List<FileChange>,
    val diffs: List<FileDiff>,
    val risks: List<String>,
    val riskLevel: String = "Low", // Low, Medium, High
    val validationRequired: Boolean = true,
    val explanation: String,
    var approved: Boolean = false,
    var status: String = "Pending" // Pending, Validated, Approved, Applied, Rejected
)

data class ChangeSnapshot(
    val snapshotId: String,
    val operationId: String,
    val projectId: String,
    val timestamp: Long,
    val previousFiles: Map<String, String?> // filePath -> previousContent (null if file didn't exist)
)

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

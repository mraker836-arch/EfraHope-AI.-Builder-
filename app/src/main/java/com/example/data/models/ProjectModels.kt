package com.example.data.models

import com.example.data.db.ProjectFileEntity
import java.util.UUID

enum class ErrorType {
    SYNTAX,
    TYPE,
    IMPORT,
    ROUTE,
    COMPONENT,
    CONFIGURATION,
    RUNTIME,
    BUILD,
    DEPENDENCY,
    SCHEMA,
    DATABASE,
    UNKNOWN
}

enum class ErrorSeverity {
    ERROR,
    WARNING,
    INFO
}

enum class ErrorStatus {
    OPEN,
    ANALYZING,
    FIX_PROPOSED,
    FIXED,
    IGNORED,
    FAILED
}

enum class ProjectHealth {
    HEALTHY,
    WARNING,
    CRITICAL,
    UNKNOWN
}

enum class FixConfidence {
    HIGH,
    MEDIUM,
    LOW
}

data class AppError(
    val id: String = UUID.randomUUID().toString(),
    val type: ErrorType = ErrorType.UNKNOWN,
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    val message: String,
    val source: String = "ValidationEngine",
    val file: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val code: String? = null,
    val stack: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: ErrorStatus = ErrorStatus.OPEN,
    val relatedFiles: List<String> = emptyList(),
    val possibleCause: String? = null,
    val suggestedSolution: String? = null,
    val rootErrorId: String? = null,
    val resolved: Boolean = (status == ErrorStatus.FIXED)
)

data class FixProposal(
    val errorId: String,
    val rootCause: String,
    val explanation: String,
    val changePlan: com.example.data.ai.models.ChangePlan,
    val confidence: FixConfidence = FixConfidence.HIGH,
    val confidenceReason: String = "Direct path resolution and minimal edit.",
    val affectedFiles: List<String> = emptyList(),
    val expectedResult: String = "Resolves error and satisfies validation.",
    val risk: String = "Low",
    val isDestructive: Boolean = false,
    val validationResult: com.example.data.ai.models.ValidationResult? = null
)

data class ValidationHistoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val validationType: String = "FULL", // "FULL", "TARGETED", "BUILD"
    val isSuccess: Boolean,
    val errorsCount: Int,
    val warningsCount: Int,
    val durationMs: Long,
    val triggeredBy: String = "User", // "User", "AutoFix", "AIChange", "Build"
    val fixedErrorsCount: Int = 0
)

enum class AIOperationState {
    IDLE,
    ANALYZING,
    PLANNING,
    GENERATING,
    MODIFYING,
    VALIDATING,
    ERROR
}

data class ProjectSettings(
    val theme: String = "Dark",
    val enableAIAutoFix: Boolean = false,
    val buildTarget: String = "Web & Mobile",
    val customSettings: Map<String, String> = emptyMap()
)

data class ProjectModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val ownerId: String = "dev-user-1",
    val members: List<com.example.data.auth.models.ProjectMember> = emptyList(),
    val appType: String = "Web App",
    val style: String = "Sleek Slate",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "Ready",
    val buildStatus: String = "IDLE",
    val testingStatus: String = "IDLE",
    val health: ProjectHealth = ProjectHealth.HEALTHY,
    val files: List<ProjectFileEntity> = emptyList(),
    val folders: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val pages: List<String> = emptyList(),
    val components: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val routes: List<String> = emptyList(),
    val settings: ProjectSettings = ProjectSettings(),
    val errors: List<AppError> = emptyList(),
    val warnings: List<AppError> = emptyList(),
    val validationHistory: List<ValidationHistoryRecord> = emptyList()
)

fun com.example.data.db.ProjectEntity.toProjectModel(): ProjectModel {
    return ProjectModel(
        id = this.id,
        name = this.name,
        description = this.description,
        ownerId = this.ownerId,
        appType = this.appType,
        style = this.style,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        status = this.status,
        buildStatus = this.buildStatus,
        testingStatus = this.testingStatus
    )
}


package com.example.data.ai

data class AgentStep(
    val agentName: String,
    val actionName: String,
    val description: String,
    val status: StepStatus = StepStatus.COMPLETED
)

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class GenerationPlan(
    val title: String,
    val requirements: List<String>,
    val pages: List<String>,
    val components: List<String>,
    val dataModels: List<String>,
    val userRoles: List<String>,
    val steps: List<AgentStep>
)

data class GeneratedFile(
    val filePath: String,
    val content: String,
    val language: String,
    val isMain: Boolean = false
)

data class AgentResponse(
    val message: String,
    val agentName: String,
    val plan: GenerationPlan? = null,
    val files: List<GeneratedFile> = emptyList(),
    val actionType: String = "info"
)

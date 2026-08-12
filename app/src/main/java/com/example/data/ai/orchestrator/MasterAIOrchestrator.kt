package com.example.data.ai.orchestrator

import com.example.data.ai.agents.DatabasePlannerAgent
import com.example.data.ai.agents.CodeGeneratorAgent
import com.example.data.ai.agents.ProjectPlannerAgent
import com.example.data.ai.agents.UIGeneratorAgent
import com.example.data.ai.context.ProjectContextBuilder
import com.example.data.ai.models.*
import com.example.data.ai.operation.AIOperationManager
import com.example.data.ai.operation.RollbackManager
import com.example.data.ai.operation.SelfCorrectionManager
import com.example.data.ai.service.AIService
import com.example.data.ai.validation.CodeValidator
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AIOperationState

enum class IntentType {
    GENERAL_QUESTION,
    PROJECT_ANALYSIS,
    PROJECT_PLAN,
    DATABASE_PLAN,
    SCHEMA_MODIFICATION,
    API_PLAN,
    DATA_MODEL_REQUEST,
    CREATE_FILE,
    CREATE_COMPONENT,
    CREATE_PAGE,
    MODIFY_FILE,
    MODIFY_UI,
    REFACTOR,
    FIX_CODE,
    EXPLAIN_CODE,
    RELEASE_ANALYSIS,
    RELEASE_NOTES,
    RELEASE_READINESS
}

class MasterAIOrchestrator(
    private val aiService: AIService = AIService()
) {
    val validationEngine = com.example.data.ai.validation.ValidationEngine()
    val databasePlannerAgent = DatabasePlannerAgent(aiService)
    private val plannerAgent = ProjectPlannerAgent(aiService)
    private val codeGeneratorAgent = CodeGeneratorAgent(aiService)
    private val uiGeneratorAgent = UIGeneratorAgent(aiService)

    fun detectIntent(userPrompt: String): IntentType {
        val lower = userPrompt.lowercase()
        return when {
            lower.contains("release notes") -> IntentType.RELEASE_NOTES
            lower.contains("ready for production") || lower.contains("release readiness") || lower.contains("readiness score") -> IntentType.RELEASE_READINESS
            lower.contains("release analysis") || lower.contains("production readiness") || lower.contains("release check") -> IntentType.RELEASE_ANALYSIS
            lower.contains("schema modification") || lower.contains("add column") || lower.contains("add field") || lower.contains("loyalty points") -> IntentType.SCHEMA_MODIFICATION
            lower.contains("database") || lower.contains("schema") || lower.contains("entities") || lower.contains("data model") || lower.contains("tables") -> IntentType.DATABASE_PLAN
            lower.contains("api") || lower.contains("endpoints") || lower.contains("route contract") -> IntentType.API_PLAN
            lower.contains("plan") || lower.contains("architecture") || lower.contains("blueprint") || lower.contains("design app") || lower.contains("build an e-commerce") || lower.contains("e-commerce dashboard") -> IntentType.PROJECT_PLAN
            lower.contains("login page") || lower.contains("create page") || lower.contains("auth page") || lower.contains("add page") -> IntentType.CREATE_PAGE
            lower.contains("create component") || lower.contains("add component") || lower.contains("dialog component") || lower.contains("confirmation dialog") -> IntentType.CREATE_COMPONENT
            lower.contains("search bar") || lower.contains("modify ui") || lower.contains("add to dashboard") || lower.contains("update layout") || lower.contains("style") -> IntentType.MODIFY_UI
            lower.contains("create file") || lower.contains("create service") || lower.contains("create hook") || lower.contains("add file") || lower.contains("add utility") || lower.contains("trade service") -> IntentType.CREATE_FILE
            lower.contains("analyze") || lower.contains("audit") || lower.contains("review") -> IntentType.PROJECT_ANALYSIS
            lower.contains("refactor") -> IntentType.REFACTOR
            lower.contains("fix") || lower.contains("bug") || lower.contains("error") -> IntentType.FIX_CODE
            lower.contains("explain") || lower.contains("how does") || lower.contains("understand") -> IntentType.EXPLAIN_CODE
            lower.contains("modify file") || lower.contains("update code") || lower.contains("edit file") -> IntentType.MODIFY_FILE
            lower.contains("analyze") || lower.contains("audit") || lower.contains("review") -> IntentType.PROJECT_ANALYSIS
            else -> IntentType.GENERAL_QUESTION
        }
    }

    suspend fun processRequest(
        userPrompt: String,
        project: ProjectEntity?,
        files: List<ProjectFileEntity>,
        activeFileId: String? = null
    ): AIResult<Any> {
        val projectId = project?.id ?: "global"
        val projectName = project?.name ?: "EfraHope AI Workspace"

        val intent = detectIntent(userPrompt)

        return when (intent) {
            IntentType.DATABASE_PLAN, IntentType.API_PLAN, IntentType.DATA_MODEL_REQUEST -> {
                AIOperationManager.startOperation(
                    type = "DATABASE_PLANNING",
                    projectId = projectId,
                    projectName = projectName,
                    initialState = AIOperationState.ANALYZING,
                    statusMessage = "Analyzing database requirements and data entities..."
                )

                val (schema, contract) = databasePlannerAgent.planDatabase(userPrompt, projectName)
                AIOperationManager.completeOperation(projectName = projectName, success = true)

                AIResult(
                    success = true,
                    type = "database_schema_result",
                    message = "Database Schema & API Contracts generated successfully.",
                    data = Pair(schema, contract)
                )
            }

            IntentType.SCHEMA_MODIFICATION -> {
                AIOperationManager.startOperation(
                    type = "SCHEMA_MODIFICATION",
                    projectId = projectId,
                    projectName = projectName,
                    initialState = AIOperationState.ANALYZING,
                    statusMessage = "Analyzing schema change prompt and evaluating impact..."
                )

                val (currentSchema, _) = databasePlannerAgent.planDatabase(userPrompt, projectName)
                val changePlan = databasePlannerAgent.planSchemaChange(currentSchema, userPrompt)
                AIOperationManager.completeOperation(projectName = projectName, success = true)

                AIResult(
                    success = true,
                    type = "schema_change_plan",
                    message = "Schema Change Plan generated for review.",
                    data = changePlan
                )
            }

            IntentType.PROJECT_PLAN -> {
                AIOperationManager.startOperation(
                    type = "PROJECT_PLANNING",
                    projectId = projectId,
                    projectName = projectName,
                    initialState = AIOperationState.ANALYZING,
                    statusMessage = "Analyzing prompt and constructing project context..."
                )

                val context = ProjectContextBuilder.buildContext(project, files, activeFileId)

                AIOperationManager.updateState(
                    state = AIOperationState.PLANNING,
                    statusMessage = "Synthesizing structured project plan..."
                )

                val planResult = plannerAgent.createPlan(userPrompt, context)

                AIOperationManager.updateState(
                    state = AIOperationState.VALIDATING,
                    statusMessage = "Validating generated plan structure..."
                )

                if (planResult.success && planResult.data != null) {
                    AIOperationManager.completeOperation(projectName = projectName, success = true)
                    AIResult(
                        success = true,
                        type = "planner_result",
                        message = "Project Plan ready for review.",
                        data = planResult.data
                    )
                } else {
                    val errMsg = planResult.message
                    AIOperationManager.completeOperation(projectName = projectName, success = false, errorMessage = errMsg)
                    AIResult(
                        success = false,
                        type = "planner_result",
                        message = "Planning failed: $errMsg",
                        error = planResult.error
                    )
                }
            }

            IntentType.CREATE_PAGE, IntentType.CREATE_COMPONENT, IntentType.MODIFY_UI -> {
                AIOperationManager.startOperation(
                    type = "UI_GENERATION",
                    projectId = projectId,
                    projectName = projectName,
                    initialState = AIOperationState.ANALYZING,
                    statusMessage = "Analyzing UI components & layout hierarchy..."
                )

                val context = ProjectContextBuilder.buildContext(project, files, activeFileId)

                AIOperationManager.updateState(
                    state = AIOperationState.GENERATING,
                    statusMessage = "Generating UI component & page layout..."
                )

                val changePlan = uiGeneratorAgent.generateUIChange(userPrompt, intent.name, context, files)

                AIOperationManager.updateState(
                    state = AIOperationState.VALIDATING,
                    statusMessage = "Validating UI code syntax & dependencies..."
                )

                val validation = CodeValidator.validateChanges(changePlan.changes)
                val (finalPlan, finalValidation) = if (!validation.isValid) {
                    SelfCorrectionManager.attemptCorrection(changePlan, validation)
                } else {
                    Pair(changePlan, validation)
                }

                // Prepare rollback snapshot
                RollbackManager.createSnapshot(
                    operationId = finalPlan.operationId,
                    projectId = projectId,
                    affectedFiles = finalPlan.affectedFiles,
                    existingFiles = files
                )

                AIOperationManager.completeOperation(projectName = projectName, success = finalValidation.isValid)

                AIResult(
                    success = finalValidation.isValid,
                    type = "change_plan",
                    message = if (finalValidation.isValid) "Change Plan ready for review." else "Validation failed: ${finalValidation.errors.joinToString()}",
                    data = finalPlan,
                    error = if (!finalValidation.isValid) AIError("VALIDATION_ERROR", finalValidation.errors.joinToString()) else null
                )
            }

            IntentType.CREATE_FILE, IntentType.MODIFY_FILE, IntentType.REFACTOR, IntentType.FIX_CODE -> {
                AIOperationManager.startOperation(
                    type = "CODE_GENERATION",
                    projectId = projectId,
                    projectName = projectName,
                    initialState = AIOperationState.ANALYZING,
                    statusMessage = "Inspecting project code architecture..."
                )

                val context = ProjectContextBuilder.buildContext(project, files, activeFileId)

                AIOperationManager.updateState(
                    state = AIOperationState.GENERATING,
                    statusMessage = "Generating code modification..."
                )

                val changePlan = codeGeneratorAgent.generateCodeChange(userPrompt, intent.name, context, files)

                AIOperationManager.updateState(
                    state = AIOperationState.VALIDATING,
                    statusMessage = "Validating code syntax & imports..."
                )

                val validation = CodeValidator.validateChanges(changePlan.changes)
                val (finalPlan, finalValidation) = if (!validation.isValid) {
                    SelfCorrectionManager.attemptCorrection(changePlan, validation)
                } else {
                    Pair(changePlan, validation)
                }

                RollbackManager.createSnapshot(
                    operationId = finalPlan.operationId,
                    projectId = projectId,
                    affectedFiles = finalPlan.affectedFiles,
                    existingFiles = files
                )

                AIOperationManager.completeOperation(projectName = projectName, success = finalValidation.isValid)

                AIResult(
                    success = finalValidation.isValid,
                    type = "change_plan",
                    message = if (finalValidation.isValid) "Change Plan ready for review." else "Validation failed: ${finalValidation.errors.joinToString()}",
                    data = finalPlan,
                    error = if (!finalValidation.isValid) AIError("VALIDATION_ERROR", finalValidation.errors.joinToString()) else null
                )
            }

            IntentType.RELEASE_ANALYSIS, IntentType.RELEASE_READINESS, IntentType.RELEASE_NOTES -> {
                AIOperationManager.startOperation(
                    type = intent.name,
                    projectId = projectId,
                    projectName = projectName,
                    initialState = AIOperationState.ANALYZING,
                    statusMessage = "Evaluating release candidate readiness, build logs, quality gates & database state..."
                )

                val validator = com.example.data.release.service.ReleaseValidator()
                val readiness = validator.validateProjectForRelease(
                    version = "1.0.0",
                    environment = com.example.data.release.models.ReleaseEnvironment.PRODUCTION,
                    files = files,
                    databaseSchema = null
                )

                AIOperationManager.completeOperation(projectName = projectName, success = true)

                val responseText = buildString {
                    append("### Release Readiness Evaluation\n")
                    append("**Status**: ${readiness.readinessLevel}\n")
                    append("**Score**: ${readiness.scorePercent?.let { "$it%" } ?: "UNKNOWN"}\n\n")
                    append("**Summary**: ${readiness.summary}\n\n")
                    append("#### Quality Gates:\n")
                    readiness.gates.forEach { gate ->
                        append("- **${gate.gate.name}**: ${gate.status.name} - ${gate.message}\n")
                    }
                    if (readiness.blockers.isNotEmpty()) {
                        append("\n#### Release Blockers:\n")
                        readiness.blockers.forEach { blocker ->
                            append("- **${blocker.title}**: ${blocker.description} (Resolution: ${blocker.resolutionSteps})\n")
                        }
                    }
                }

                AIResult(
                    success = true,
                    type = "release_readiness_result",
                    message = "Release readiness analysis complete.",
                    data = responseText
                )
            }

            IntentType.PROJECT_ANALYSIS, IntentType.EXPLAIN_CODE -> {
                AIOperationManager.startOperation(
                    type = "PROJECT_ANALYSIS",
                    projectId = projectId,
                    projectName = projectName,
                    initialState = AIOperationState.ANALYZING,
                    statusMessage = "Analyzing project files..."
                )

                val context = ProjectContextBuilder.buildContext(project, files, activeFileId)
                val analysisResult = aiService.analyze(context)

                AIOperationManager.completeOperation(projectName = projectName, success = analysisResult.success, errorMessage = analysisResult.message)

                AIResult(
                    success = analysisResult.success,
                    type = "analysis_result",
                    message = analysisResult.message,
                    data = analysisResult.data ?: analysisResult.message
                )
            }

            IntentType.GENERAL_QUESTION -> {
                AIOperationManager.startOperation(
                    type = "GENERAL_QUESTION",
                    projectId = projectId,
                    projectName = projectName,
                    initialState = AIOperationState.GENERATING,
                    statusMessage = "Generating response..."
                )

                val context = ProjectContextBuilder.buildContext(project, files, activeFileId)
                val promptWithContext = "Project Context:\n$context\n\nUser Question:\n$userPrompt"
                val genResult = aiService.generate(promptWithContext)

                AIOperationManager.completeOperation(projectName = projectName, success = genResult.success, errorMessage = genResult.message)

                AIResult(
                    success = genResult.success,
                    type = "text_response",
                    message = genResult.message,
                    data = genResult.data ?: genResult.message
                )
            }
        }
    }
}

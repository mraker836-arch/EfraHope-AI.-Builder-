package com.example.data.ai.operation

import com.example.data.ai.agents.ErrorFixingAgent
import com.example.data.ai.validation.ValidationEngine
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.*
import java.util.UUID

data class FixExecutionResult(
    val isSuccess: Boolean,
    val message: String,
    val fixedErrorId: String,
    val remainingErrorsCount: Int,
    val rolledBack: Boolean = false,
    val requiresUserApproval: Boolean = false,
    val fixProposal: FixProposal? = null
)

class FixWorkflowManager(
    private val errorFixingAgent: ErrorFixingAgent = ErrorFixingAgent(),
    private val validationEngine: ValidationEngine = ValidationEngine()
) {
    private val attemptCounts = mutableMapOf<String, Int>()
    var autoFixEnabled: Boolean = false
    val maxFixAttempts: Int = 3

    fun canAutoFix(proposal: FixProposal): Boolean {
        if (!autoFixEnabled) return false
        return proposal.confidence == FixConfidence.HIGH &&
               proposal.risk == "Low" &&
               !proposal.isDestructive &&
               proposal.affectedFiles.size <= 2 &&
               proposal.validationResult?.isValid == true
    }

    fun processErrorFix(
        error: AppError,
        project: ProjectEntity,
        files: List<ProjectFileEntity>,
        currentErrors: List<AppError>,
        applyFixCallback: suspend (com.example.data.ai.models.ChangePlan) -> Boolean,
        rollbackCallback: suspend () -> Unit,
        saveHistoryCallback: suspend (ValidationHistoryRecord) -> Unit
    ): FixExecutionResult {
        val currentAttempts = attemptCounts.getOrDefault(error.id, 0)
        if (currentAttempts >= maxFixAttempts) {
            return FixExecutionResult(
                isSuccess = false,
                message = "Maximum fix attempts ($maxFixAttempts) reached for error '${error.id}'. Manual review required.",
                fixedErrorId = error.id,
                remainingErrorsCount = currentErrors.size
            )
        }

        attemptCounts[error.id] = currentAttempts + 1

        // 1. Generate Fix Proposal & Root Cause Analysis
        val proposal = errorFixingAgent.analyzeAndProposeFix(error, project, files)

        // 2. Safety Check for Auto-Fix
        if (!canAutoFix(proposal)) {
            return FixExecutionResult(
                isSuccess = false,
                message = "Fix proposal generated. User review required (${proposal.confidence} confidence, ${proposal.risk} risk).",
                fixedErrorId = error.id,
                remainingErrorsCount = currentErrors.size,
                requiresUserApproval = true,
                fixProposal = proposal
            )
        }

        // 3. Automated Application and Re-validation Loop
        val initialErrorCount = currentErrors.size
        var appliedSuccess = false

        try {
            // Apply Change Plan
            appliedSuccess = kotlinx.coroutines.runBlocking {
                applyFixCallback(proposal.changePlan)
            }
        } catch (e: Exception) {
            appliedSuccess = false
        }

        if (!appliedSuccess) {
            return FixExecutionResult(
                isSuccess = false,
                message = "Failed to apply fix change plan.",
                fixedErrorId = error.id,
                remainingErrorsCount = initialErrorCount
            )
        }

        // 4. Re-validate Project Post-Fix
        val revalidation = validationEngine.validateProject(project, files)

        val historyRecord = ValidationHistoryRecord(
            id = UUID.randomUUID().toString(),
            projectId = project.id,
            timestamp = System.currentTimeMillis(),
            validationType = "AUTO_FIX_REVALIDATE",
            isSuccess = revalidation.isValid,
            errorsCount = revalidation.errors.size,
            warningsCount = revalidation.warnings.size,
            durationMs = revalidation.durationMs,
            triggeredBy = if (autoFixEnabled) "AutoFix" else "User",
            fixedErrorsCount = if (revalidation.errors.size < initialErrorCount) 1 else 0
        )

        kotlinx.coroutines.runBlocking {
            saveHistoryCallback(historyRecord)
        }

        // 5. Compare Results
        if (revalidation.errors.size < initialErrorCount || !revalidation.errors.any { it.id == error.id }) {
            return FixExecutionResult(
                isSuccess = true,
                message = "1 error resolved successfully.",
                fixedErrorId = error.id,
                remainingErrorsCount = revalidation.errors.size
            )
        } else if (revalidation.errors.size > initialErrorCount) {
            // Rollback introduced issues
            kotlinx.coroutines.runBlocking {
                rollbackCallback()
            }
            return FixExecutionResult(
                isSuccess = false,
                message = "Fix introduced new validation issues. Changes rolled back automatically.",
                fixedErrorId = error.id,
                remainingErrorsCount = initialErrorCount,
                rolledBack = true
            )
        } else {
            return FixExecutionResult(
                isSuccess = false,
                message = "Fix did not resolve target error.",
                fixedErrorId = error.id,
                remainingErrorsCount = revalidation.errors.size
            )
        }
    }

    fun resetAttemptCount(errorId: String) {
        attemptCounts.remove(errorId)
    }
}

package com.example.data.ai.operation

import com.example.data.ai.models.ChangePlan
import com.example.data.ai.models.ChangeOperation
import com.example.data.ai.models.FileChange
import com.example.data.ai.models.ValidationResult
import com.example.data.ai.utils.DiffGenerator
import com.example.data.ai.validation.CodeValidator

object SelfCorrectionManager {

    private const val MAX_RETRY_LIMIT = 2

    fun attemptCorrection(
        plan: ChangePlan,
        initialValidation: ValidationResult
    ): Pair<ChangePlan, ValidationResult> {
        var currentPlan = plan
        var currentValidation = initialValidation
        var retries = 0

        while (!currentValidation.isValid && retries < MAX_RETRY_LIMIT) {
            retries++

            // Apply targeted syntax corrections to changes
            val correctedChanges = currentPlan.changes.map { change ->
                if (change.content != null) {
                    val fixedContent = fixCommonSyntaxErrors(change.content, currentValidation.errors)
                    change.copy(content = fixedContent)
                } else {
                    change
                }
            }

            // Regenerate diffs for corrected changes
            val correctedDiffs = correctedChanges.map { change ->
                DiffGenerator.generateDiff(
                    change.targetFilePath,
                    change.oldContent,
                    change.content,
                    change.operation
                )
            }

            currentPlan = currentPlan.copy(
                changes = correctedChanges,
                diffs = correctedDiffs,
                explanation = "${currentPlan.explanation} [Self-Corrected Attempt #$retries]"
            )

            currentValidation = CodeValidator.validateChanges(currentPlan.changes)
        }

        return Pair(currentPlan, currentValidation)
    }

    private fun fixCommonSyntaxErrors(code: String, errors: List<String>): String {
        var fixed = code
        errors.forEach { err ->
            if (err.contains("curly braces")) {
                var braceCount = 0
                for (ch in fixed) {
                    if (ch == '{') braceCount++
                    if (ch == '}') braceCount--
                }
                while (braceCount > 0) {
                    fixed += "\n}"
                    braceCount--
                }
            }
            if (err.contains("brackets")) {
                var bracketCount = 0
                for (ch in fixed) {
                    if (ch == '[') bracketCount++
                    if (ch == ']') bracketCount--
                }
                while (bracketCount > 0) {
                    fixed += "]"
                    bracketCount--
                }
            }
            if (err.contains("parentheses")) {
                var parenCount = 0
                for (ch in fixed) {
                    if (ch == '(') parenCount++
                    if (ch == ')') parenCount--
                }
                while (parenCount > 0) {
                    fixed += ")"
                    parenCount--
                }
            }
        }
        return fixed
    }
}

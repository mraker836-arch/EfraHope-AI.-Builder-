package com.example.data.ai.validation

import com.example.data.ai.models.ChangeOperation
import com.example.data.ai.models.FileChange
import com.example.data.ai.models.ValidationResult

object CodeValidator {

    fun validateFileChange(change: FileChange): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (change.targetFilePath.isBlank()) {
            errors.add("Target file path cannot be blank.")
        }

        if (change.targetFilePath.contains("..") || change.targetFilePath.startsWith("/")) {
            errors.add("Invalid relative file path: ${change.targetFilePath}")
        }

        when (change.operation) {
            ChangeOperation.CREATE_FILE -> {
                val content = change.content
                if (content.isNullOrBlank()) {
                    errors.add("New file content for '${change.targetFilePath}' cannot be empty.")
                } else {
                    validateCodeSyntax(content, errors, warnings)
                }
            }

            ChangeOperation.UPDATE_FILE -> {
                val content = change.content
                if (content.isNullOrBlank()) {
                    errors.add("Updated file content for '${change.targetFilePath}' cannot be empty.")
                } else {
                    validateCodeSyntax(content, errors, warnings)
                }
            }

            ChangeOperation.RENAME_FILE -> {
                if (change.newFilePath.isNullOrBlank()) {
                    errors.add("New file path for rename operation on '${change.targetFilePath}' must be specified.")
                }
            }

            ChangeOperation.DELETE_FILE -> {
                warnings.add("Deletion of file '${change.targetFilePath}' is a destructive action.")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    fun validateChanges(changes: List<FileChange>): ValidationResult {
        val allErrors = mutableListOf<String>()
        val allWarnings = mutableListOf<String>()

        changes.forEach { change ->
            val res = validateFileChange(change)
            allErrors.addAll(res.errors)
            allWarnings.addAll(res.warnings)
        }

        return ValidationResult(
            isValid = allErrors.isEmpty(),
            errors = allErrors,
            warnings = allWarnings
        )
    }

    private fun validateCodeSyntax(code: String, errors: MutableList<String>, warnings: MutableList<String>) {
        // Check bracket / brace / paren matching
        var braceCount = 0
        var bracketCount = 0
        var parenCount = 0

        for (ch in code) {
            when (ch) {
                '{' -> braceCount++
                '}' -> braceCount--
                '[' -> bracketCount++
                ']' -> bracketCount--
                '(' -> parenCount++
                ')' -> parenCount--
            }
        }

        if (braceCount != 0) {
            errors.add("Unbalanced curly braces ({}) in code snippet.")
        }
        if (bracketCount != 0) {
            errors.add("Unbalanced square brackets ([]) in code snippet.")
        }
        if (parenCount != 0) {
            errors.add("Unbalanced parentheses (()) in code snippet.")
        }

        if (code.contains("<<<<<<<") || code.contains("=======") || code.contains(">>>>>>>")) {
            errors.add("Code contains unresolved git merge conflict markers.")
        }
    }
}

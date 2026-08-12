package com.example.data.preview.runtime

import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.ErrorType
import com.example.data.preview.PreviewLogEntry

class RuntimeErrorHandler {

    fun handleException(
        throwable: Throwable,
        sourceFile: String? = null,
        lineNumber: Int? = null
    ): AppError {
        val message = throwable.message ?: throwable.localizedMessage ?: "Uncaught runtime exception in preview"
        val stack = throwable.stackTraceToString()

        val inferredType = when {
            message.contains("is not defined", ignoreCase = true) -> ErrorType.COMPONENT
            message.contains("Cannot find module", ignoreCase = true) || message.contains("import", ignoreCase = true) -> ErrorType.IMPORT
            message.contains("TypeError", ignoreCase = true) -> ErrorType.TYPE
            message.contains("SyntaxError", ignoreCase = true) -> ErrorType.SYNTAX
            else -> ErrorType.RUNTIME
        }

        return AppError(
            type = inferredType,
            severity = ErrorSeverity.ERROR,
            message = "Runtime Error: $message",
            source = "PreviewRuntime",
            file = sourceFile,
            line = lineNumber,
            stack = stack,
            possibleCause = "Unexpected execution state in component or missing runtime state.",
            suggestedSolution = "Inspect runtime imports and component definition."
        )
    }

    fun handleConsoleError(
        message: String,
        sourceFile: String? = null,
        lineNumber: Int? = null
    ): AppError {
        val inferredType = when {
            message.contains("is not defined", ignoreCase = true) -> ErrorType.COMPONENT
            message.contains("Cannot find module", ignoreCase = true) -> ErrorType.IMPORT
            else -> ErrorType.RUNTIME
        }

        return AppError(
            type = inferredType,
            severity = ErrorSeverity.ERROR,
            message = "Preview Console Error: $message",
            source = "PreviewRuntime",
            file = sourceFile,
            line = lineNumber,
            possibleCause = "Console error output captured during preview execution.",
            suggestedSolution = "Review referenced variables or components in source code."
        )
    }

    fun formatLogEntry(error: AppError): PreviewLogEntry {
        return PreviewLogEntry(
            level = if (error.severity == ErrorSeverity.ERROR) "ERROR" else "WARN",
            message = error.message,
            source = error.source,
            file = error.file,
            line = error.line
        )
    }
}

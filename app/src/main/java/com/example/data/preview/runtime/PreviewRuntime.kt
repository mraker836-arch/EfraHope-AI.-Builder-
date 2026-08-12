package com.example.data.preview.runtime

import com.example.data.models.AppError
import com.example.data.preview.PreviewLogEntry
import com.example.data.preview.PreviewProject
import java.util.UUID

data class RuntimeExecutionResult(
    val isSuccess: Boolean,
    val sessionId: String,
    val activeUrl: String,
    val activeComponentCount: Int,
    val errors: List<AppError> = emptyList(),
    val warnings: List<AppError> = emptyList(),
    val logs: List<PreviewLogEntry> = emptyList()
)

class PreviewRuntime(
    private val errorHandler: RuntimeErrorHandler = RuntimeErrorHandler()
) {
    private var activeSessionId: String? = null
    private var isRunning: Boolean = false
    private val currentLogs = mutableListOf<PreviewLogEntry>()

    fun startRuntime(project: PreviewProject, bundle: CompiledBundle): RuntimeExecutionResult {
        stopRuntime() // Safely terminate any previous runtime session

        val newSessionId = UUID.randomUUID().toString()
        activeSessionId = newSessionId
        isRunning = true
        currentLogs.clear()

        val startLog = PreviewLogEntry(
            level = "INFO",
            message = "Sandboxed Runtime Session $newSessionId started for project '${project.name}'.",
            source = "PreviewRuntime"
        )
        currentLogs.add(startLog)

        // Check if bundle has critical errors
        if (!bundle.isSuccess) {
            isRunning = false
            val failLog = PreviewLogEntry(
                level = "ERROR",
                message = "Failed to launch sandboxed runtime: Build bundle contains ${bundle.errors.size} errors.",
                source = "PreviewRuntime"
            )
            currentLogs.add(failLog)

            return RuntimeExecutionResult(
                isSuccess = false,
                sessionId = newSessionId,
                activeUrl = "https://sandbox.efrahope.ai/preview/error",
                activeComponentCount = 0,
                errors = bundle.errors,
                warnings = bundle.warnings,
                logs = currentLogs.toList()
            )
        }

        val componentCount = project.files.count { it.path.contains("component", ignoreCase = true) || it.path.endsWith(".tsx") || it.path.endsWith(".jsx") }

        val successLog = PreviewLogEntry(
            level = "INFO",
            message = "Sandboxed preview active at https://sandbox.efrahope.ai/preview/${project.id}",
            source = "PreviewRuntime"
        )
        currentLogs.add(successLog)

        return RuntimeExecutionResult(
            isSuccess = true,
            sessionId = newSessionId,
            activeUrl = "https://sandbox.efrahope.ai/preview/${project.id}",
            activeComponentCount = if (componentCount > 0) componentCount else 1,
            errors = emptyList(),
            warnings = bundle.warnings,
            logs = currentLogs.toList()
        )
    }

    fun triggerRuntimeErrorForTesting(message: String, file: String? = "src/App.tsx"): AppError {
        val error = errorHandler.handleConsoleError(message, sourceFile = file)
        val log = errorHandler.formatLogEntry(error)
        currentLogs.add(log)
        return error
    }

    fun stopRuntime() {
        if (isRunning && activeSessionId != null) {
            currentLogs.add(
                PreviewLogEntry(
                    level = "INFO",
                    message = "Preview session $activeSessionId safely stopped and terminated.",
                    source = "PreviewRuntime"
                )
            )
        }
        isRunning = false
        activeSessionId = null
    }

    fun isRunning(): Boolean = isRunning
    fun getActiveSessionId(): String? = activeSessionId
}

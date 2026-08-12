package com.example.data.preview.runtime

import com.example.data.ai.validation.ValidationEngine
import com.example.data.models.AppError
import com.example.data.preview.PreviewProject

data class EngineExecutionResult(
    val isSuccess: Boolean,
    val sessionId: String,
    val previewUrl: String,
    val activeComponentCount: Int,
    val compileDurationMs: Long,
    val errors: List<AppError> = emptyList(),
    val warnings: List<AppError> = emptyList(),
    val missingDependencies: List<String> = emptyList()
)

class RuntimeEngine(
    private val validationEngine: ValidationEngine? = null
) {
    val dependencyResolver = DependencyResolver()
    val projectTransformer = ProjectTransformer()
    val projectCompiler = ProjectCompiler(validationEngine)
    val errorHandler = RuntimeErrorHandler()
    val previewRuntime = PreviewRuntime(errorHandler)

    fun buildAndRun(project: PreviewProject): EngineExecutionResult {
        val startTime = System.currentTimeMillis()

        // 1. Dependency Resolution
        val depResult = dependencyResolver.resolveDependencies(project)

        // 2. Project Transformation
        val transformed = projectTransformer.transform(project)

        // 3. Project Compilation & Validation
        val compiledBundle = projectCompiler.compile(transformed.project)

        val allErrors = mutableListOf<AppError>()
        allErrors.addAll(depResult.errors)
        allErrors.addAll(compiledBundle.errors)

        val allWarnings = mutableListOf<AppError>()
        allWarnings.addAll(depResult.warnings)
        allWarnings.addAll(compiledBundle.warnings)

        if (!compiledBundle.isSuccess || allErrors.isNotEmpty()) {
            return EngineExecutionResult(
                isSuccess = false,
                sessionId = "failed-build",
                previewUrl = "https://sandbox.efrahope.ai/preview/error",
                activeComponentCount = 0,
                compileDurationMs = System.currentTimeMillis() - startTime,
                errors = allErrors,
                warnings = allWarnings,
                missingDependencies = depResult.missingDependencies
            )
        }

        // 4. Launch Preview Runtime
        val runtimeResult = previewRuntime.startRuntime(transformed.project, compiledBundle)

        return EngineExecutionResult(
            isSuccess = runtimeResult.isSuccess,
            sessionId = runtimeResult.sessionId,
            previewUrl = runtimeResult.activeUrl,
            activeComponentCount = runtimeResult.activeComponentCount,
            compileDurationMs = compiledBundle.compilationDurationMs,
            errors = runtimeResult.errors,
            warnings = allWarnings,
            missingDependencies = depResult.missingDependencies
        )
    }

    fun stop() {
        previewRuntime.stopRuntime()
    }
}

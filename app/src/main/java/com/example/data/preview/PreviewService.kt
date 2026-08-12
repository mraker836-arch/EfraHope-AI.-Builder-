package com.example.data.preview

import com.example.data.ai.validation.ValidationEngine
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.preview.runtime.RuntimeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PreviewService(
    private val validationEngine: ValidationEngine? = null,
    private val onErrorReported: ((AppError) -> Unit)? = null
) {
    private val runtimeEngine = RuntimeEngine(validationEngine)

    private val _previewState = MutableStateFlow(PreviewState())
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()

    private var currentSession: PreviewSession? = null

    fun preparePreview(project: ProjectEntity, files: List<ProjectFileEntity>) {
        _previewState.update {
            it.copy(
                status = PreviewStatus.PREPARING,
                projectId = project.id,
                projectName = project.name
            )
        }
        val session = PreviewSession(projectId = project.id, status = PreviewStatus.PREPARING)
        session.logs.add(PreviewLogEntry(message = "Preparing preview environment for ${project.name}"))
        currentSession = session
    }

    fun startPreview(project: ProjectEntity, files: List<ProjectFileEntity>) {
        preparePreview(project, files)

        _previewState.update { it.copy(status = PreviewStatus.BUILDING) }
        currentSession?.logs?.add(PreviewLogEntry(message = "Building project preview bundle..."))

        val startTime = System.currentTimeMillis()
        val previewProject = PreviewProjectAdapter.adapt(project, files)

        _previewState.update { it.copy(status = PreviewStatus.STARTING) }

        val result = runtimeEngine.buildAndRun(previewProject)

        val duration = System.currentTimeMillis() - startTime

        if (result.isSuccess) {
            val session = currentSession?.apply {
                status = PreviewStatus.RUNNING
                lastBuildTime = System.currentTimeMillis()
                lastRefreshTime = System.currentTimeMillis()
                errors = result.errors
                warnings = result.warnings
                logs.add(PreviewLogEntry(message = "Preview launched successfully at ${result.previewUrl}"))
            }

            _previewState.update {
                it.copy(
                    status = PreviewStatus.RUNNING,
                    sessionId = result.sessionId,
                    url = result.previewUrl,
                    activeComponentCount = result.activeComponentCount,
                    lastBuildTime = System.currentTimeMillis(),
                    lastRefreshTime = System.currentTimeMillis(),
                    buildDurationMs = duration,
                    runtimeErrors = result.errors,
                    warnings = result.warnings,
                    logs = session?.logs ?: emptyList()
                )
            }
        } else {
            val session = currentSession?.apply {
                status = PreviewStatus.ERROR
                errors = result.errors
                warnings = result.warnings
                logs.add(PreviewLogEntry(level = "ERROR", message = "Preview build failed with ${result.errors.size} errors."))
            }

            // Report build/compilation errors to Error Center
            result.errors.forEach { err ->
                onErrorReported?.invoke(err)
            }

            _previewState.update {
                it.copy(
                    status = PreviewStatus.ERROR,
                    sessionId = null,
                    runtimeErrors = result.errors,
                    warnings = result.warnings,
                    logs = session?.logs ?: emptyList()
                )
            }
        }
    }

    fun stopPreview() {
        runtimeEngine.stop()
        currentSession?.status = PreviewStatus.STOPPED
        currentSession?.logs?.add(PreviewLogEntry(message = "Preview stopped by user."))

        _previewState.update {
            it.copy(
                status = PreviewStatus.STOPPED,
                logs = currentSession?.logs ?: emptyList()
            )
        }
    }

    fun refreshPreview(project: ProjectEntity, files: List<ProjectFileEntity>, isAutoRefresh: Boolean = false) {
        if (_previewState.value.status == PreviewStatus.STOPPED && isAutoRefresh) {
            return // Do not auto refresh if user stopped preview
        }

        _previewState.update { it.copy(status = PreviewStatus.REFRESHING) }
        currentSession?.logs?.add(
            PreviewLogEntry(message = if (isAutoRefresh) "Auto-refreshing preview..." else "Manual preview refresh triggered.")
        )

        val startTime = System.currentTimeMillis()
        val previewProject = PreviewProjectAdapter.adapt(project, files)

        val result = runtimeEngine.buildAndRun(previewProject)
        val duration = System.currentTimeMillis() - startTime

        if (result.isSuccess) {
            currentSession?.apply {
                status = PreviewStatus.RUNNING
                lastRefreshTime = System.currentTimeMillis()
                errors = emptyList()
                warnings = result.warnings
            }

            _previewState.update {
                it.copy(
                    status = PreviewStatus.RUNNING,
                    sessionId = result.sessionId,
                    url = result.previewUrl,
                    activeComponentCount = result.activeComponentCount,
                    lastRefreshTime = System.currentTimeMillis(),
                    buildDurationMs = duration,
                    runtimeErrors = emptyList(),
                    warnings = result.warnings
                )
            }
        } else {
            currentSession?.apply {
                status = PreviewStatus.ERROR
                errors = result.errors
            }

            result.errors.forEach { err ->
                onErrorReported?.invoke(err)
            }

            _previewState.update {
                it.copy(
                    status = PreviewStatus.ERROR,
                    runtimeErrors = result.errors,
                    warnings = result.warnings
                )
            }
        }
    }

    fun setViewportMode(mode: ViewportMode) {
        _previewState.update { it.copy(viewportMode = mode) }
    }

    fun toggleFitToScreen() {
        _previewState.update { it.copy(isFitToScreen = !it.isFitToScreen) }
    }

    fun toggleAutoRefresh() {
        _previewState.update { it.copy(autoRefreshEnabled = !it.autoRefreshEnabled) }
    }

    fun reportRuntimeError(error: AppError) {
        onErrorReported?.invoke(error)
        _previewState.update {
            val updatedErrors = it.runtimeErrors + error
            it.copy(
                status = PreviewStatus.ERROR,
                runtimeErrors = updatedErrors
            )
        }
    }

    fun getLogs(): List<PreviewLogEntry> = _previewState.value.logs
}

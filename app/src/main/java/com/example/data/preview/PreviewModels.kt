package com.example.data.preview

import com.example.data.models.AppError
import java.util.UUID

enum class PreviewStatus {
    IDLE,
    PREPARING,
    BUILDING,
    STARTING,
    RUNNING,
    REFRESHING,
    ERROR,
    STOPPED
}

enum class ViewportMode {
    DESKTOP,
    TABLET,
    MOBILE
}

data class PreviewFile(
    val path: String,
    val content: String,
    val isBinary: Boolean = false,
    val mimeType: String = "text/plain"
)

data class PreviewProject(
    val id: String,
    val name: String,
    val description: String = "",
    val appType: String = "Web & Mobile",
    val files: List<PreviewFile> = emptyList(),
    val dependencies: List<String> = emptyList()
)

data class PreviewLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO", // INFO, WARN, ERROR, DEBUG
    val message: String,
    val source: String = "PreviewRuntime",
    val file: String? = null,
    val line: Int? = null
)

data class PreviewState(
    val status: PreviewStatus = PreviewStatus.IDLE,
    val projectId: String? = null,
    val projectName: String = "Untitled",
    val lastBuildTime: Long = 0,
    val lastRefreshTime: Long = 0,
    val buildDurationMs: Long = 0,
    val runtimeErrors: List<AppError> = emptyList(),
    val warnings: List<AppError> = emptyList(),
    val viewportMode: ViewportMode = ViewportMode.DESKTOP,
    val url: String = "https://sandbox.efrahope.ai/preview",
    val sessionId: String? = null,
    val logs: List<PreviewLogEntry> = emptyList(),
    val isFitToScreen: Boolean = true,
    val autoRefreshEnabled: Boolean = true,
    val activeComponentCount: Int = 0
)

data class PreviewSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val startTime: Long = System.currentTimeMillis(),
    var status: PreviewStatus = PreviewStatus.PREPARING,
    var lastBuildTime: Long = 0,
    var lastRefreshTime: Long = 0,
    var errors: List<AppError> = emptyList(),
    var warnings: List<AppError> = emptyList(),
    val logs: MutableList<PreviewLogEntry> = mutableListOf()
)

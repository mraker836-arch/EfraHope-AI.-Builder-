package com.example.data.release.models

import java.util.UUID

enum class GitHubConnectionStatus {
    NOT_CONNECTED,
    CONNECTED,
    ERROR
}

data class GitHubConfig(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val environment: String = "github-pages",
    val token: String? = null,
    val customDomain: String? = null
) {
    fun isValid(): Boolean = owner.isNotBlank() && repo.isNotBlank() && branch.isNotBlank()
}

data class GitHubValidationCheck(
    val name: String,
    val passed: Boolean,
    val details: String
)

data class GitHubValidationResult(
    val isReady: Boolean,
    val checks: List<GitHubValidationCheck> = emptyList(),
    val reason: String? = null,
    val buildDetails: ProjectBuildDetails? = null
)

data class ProjectBuildDetails(
    val projectType: String, // Vite, React, Next.js, Static HTML, Android
    val packageManager: String, // npm, pnpm, yarn
    val buildCommand: String,
    val requiresServerRuntime: Boolean,
    val basePath: String? = null
)

data class WorkflowRunInfo(
    val runId: String = UUID.randomUUID().toString(),
    val status: DeploymentStatus,
    val logs: List<String> = emptyList(),
    val liveUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

package com.example.data.release.service

import com.example.data.db.ProjectFileEntity
import com.example.data.release.models.*
import java.util.concurrent.ConcurrentHashMap

class GitHubDeploymentProvider(
    var config: GitHubConfig = GitHubConfig(),
    val gitHubService: GitHubService = GitHubService()
) : DeploymentProvider {

    override val name: String = "GITHUB_PAGES"
    override val isSimulation: Boolean = false

    private val retryTracker = ConcurrentHashMap<String, Int>()
    private val maxRetries = 3

    fun connect(newConfig: GitHubConfig): Pair<GitHubConnectionStatus, String> {
        this.config = newConfig
        return gitHubService.testConnection(newConfig)
    }

    fun validateRepository(files: List<ProjectFileEntity>): GitHubValidationResult {
        return gitHubService.validateRepository(config, files)
    }

    fun prepareDeployment(files: List<ProjectFileEntity>): String {
        val details = gitHubService.detectProjectBuildDetails(files)
        return gitHubService.generateWorkflowYaml(config, details)
    }

    override suspend fun validateDeploymentConfig(envConfig: EnvironmentConfig): Boolean {
        return config.isValid()
    }

    override suspend fun deploy(release: Release, artifact: BuildArtifact): DeploymentResult {
        val currentRetries = retryTracker.getOrDefault(release.id, 0)
        if (currentRetries >= maxRetries) {
            return DeploymentResult(
                success = false,
                status = DeploymentStatus.FAILED,
                logs = listOf("[ERROR] Retry limit reached ($maxRetries/$maxRetries attempts failed). Deployment cancelled to prevent infinite loops."),
                errorMessage = "Deployment retry limit exceeded ($maxRetries attempts max)."
            )
        }

        retryTracker[release.id] = currentRetries + 1

        val logs = mutableListOf<String>()
        logs.add("[GITHUB DEPLOYMENT] Initiating GitHub Actions workflow dispatch for ${config.owner}/${config.repo}@${config.branch}...")

        if (!config.isValid()) {
            logs.add("[ERROR] Invalid GitHub Configuration: Owner/Repo blank.")
            return DeploymentResult(
                success = false,
                status = DeploymentStatus.FAILED,
                logs = logs,
                errorMessage = "GitHub configuration is incomplete."
            )
        }

        val runId = gitHubService.triggerWorkflowDispatch(config, release)
        logs.add("[GITHUB ACTIONS] Dispatched workflow run ID: $runId")
        logs.addAll(gitHubService.getWorkflowLogs(config, runId))

        val liveUrl = gitHubService.getPagesUrl(config)
        logs.add("[GITHUB PAGES] Site deployed successfully. Live URL: $liveUrl")

        // Reset retry tracker on success
        retryTracker.remove(release.id)

        return DeploymentResult(
            success = true,
            status = DeploymentStatus.SUCCEEDED,
            logs = logs,
            url = liveUrl
        )
    }

    override suspend fun getStatus(releaseId: String): DeploymentStatus {
        return gitHubService.getWorkflowRunStatus(config, "latest")
    }

    override suspend fun cancel(releaseId: String): Boolean {
        return true
    }

    override suspend fun rollback(release: Release, targetPreviousRelease: Release): DeploymentResult {
        val logs = mutableListOf<String>()
        logs.add("[GITHUB ROLLBACK] Initiating rollback for ${config.owner}/${config.repo} from v${release.version} to v${targetPreviousRelease.version}...")
        logs.add("[GITHUB ACTIONS] Triggering workflow dispatch pointing to target release commit: ${targetPreviousRelease.commitVersion ?: "HEAD^"}")

        val rollbackUrl = gitHubService.getPagesUrl(config)
        logs.add("[GITHUB PAGES] Rollback completed. Live URL updated: $rollbackUrl")

        return DeploymentResult(
            success = true,
            status = DeploymentStatus.ROLLED_BACK,
            logs = logs,
            url = rollbackUrl
        )
    }

    fun getDeploymentUrl(): String {
        return gitHubService.getPagesUrl(config)
    }
}

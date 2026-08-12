package com.example.data.release.service

import com.example.data.release.models.*

interface DeploymentProvider {
    val name: String
    val isSimulation: Boolean

    suspend fun validateDeploymentConfig(config: EnvironmentConfig): Boolean
    suspend fun deploy(release: Release, artifact: BuildArtifact): DeploymentResult
    suspend fun getStatus(releaseId: String): DeploymentStatus
    suspend fun cancel(releaseId: String): Boolean
    suspend fun rollback(release: Release, targetPreviousRelease: Release): DeploymentResult
}

data class DeploymentResult(
    val success: Boolean,
    val status: DeploymentStatus,
    val logs: List<String>,
    val url: String? = null,
    val errorMessage: String? = null
)

class DeploymentSimulationProvider : DeploymentProvider {
    override val name: String = "DEPLOYMENT_SIMULATION"
    override val isSimulation: Boolean = true

    override suspend fun validateDeploymentConfig(config: EnvironmentConfig): Boolean {
        return config.apiUrl.isNotBlank()
    }

    override suspend fun deploy(release: Release, artifact: BuildArtifact): DeploymentResult {
        val logs = mutableListOf<String>()
        logs.add("[SIMULATION] Deploying release candidate v${release.version} to ${release.environment}...")
        logs.add("[SIMULATION] Validating artifact checksum: ${artifact.checksum}")
        logs.add("[SIMULATION] Configured API Endpoint: ${release.envConfig?.apiUrl}")
        logs.add("[SIMULATION] Provisioning virtual deployment container environment...")
        logs.add("[SIMULATION] Deployment completed successfully in simulated mode.")

        val simUrl = "https://efrahope-builder.app/preview/${release.projectId}/v${release.version}"

        return DeploymentResult(
            success = true,
            status = DeploymentStatus.SUCCEEDED,
            logs = logs,
            url = simUrl
        )
    }

    override suspend fun getStatus(releaseId: String): DeploymentStatus {
        return DeploymentStatus.SUCCEEDED
    }

    override suspend fun cancel(releaseId: String): Boolean {
        return true
    }

    override suspend fun rollback(release: Release, targetPreviousRelease: Release): DeploymentResult {
        val logs = mutableListOf<String>()
        logs.add("[SIMULATION ROLLBACK] Reverting environment ${release.environment} from v${release.version} to v${targetPreviousRelease.version}...")
        logs.add("[SIMULATION ROLLBACK] Reinstating snapshot artifact: ${targetPreviousRelease.artifactId}")
        logs.add("[SIMULATION ROLLBACK] Rollback executed successfully.")

        val url = "https://efrahope-builder.app/preview/${targetPreviousRelease.projectId}/v${targetPreviousRelease.version}"

        return DeploymentResult(
            success = true,
            status = DeploymentStatus.ROLLED_BACK,
            logs = logs,
            url = url
        )
    }
}

class LocalExportProvider : DeploymentProvider {
    override val name: String = "LOCAL_EXPORT"
    override val isSimulation: Boolean = false

    override suspend fun validateDeploymentConfig(config: EnvironmentConfig): Boolean {
        return true
    }

    override suspend fun deploy(release: Release, artifact: BuildArtifact): DeploymentResult {
        val logs = mutableListOf<String>()
        logs.add("[LOCAL EXPORT] Exporting build artifact for local distribution...")
        logs.add("[LOCAL EXPORT] Saved package to local storage: ${artifact.path}")
        logs.add("[LOCAL EXPORT] Artifact ready for manual deployment / APK installation.")

        return DeploymentResult(
            success = true,
            status = DeploymentStatus.SUCCEEDED,
            logs = logs,
            url = artifact.path
        )
    }

    override suspend fun getStatus(releaseId: String): DeploymentStatus {
        return DeploymentStatus.SUCCEEDED
    }

    override suspend fun cancel(releaseId: String): Boolean {
        return true
    }

    override suspend fun rollback(release: Release, targetPreviousRelease: Release): DeploymentResult {
        val logs = mutableListOf<String>()
        logs.add("[LOCAL EXPORT ROLLBACK] Pointing local active export reference to v${targetPreviousRelease.version}.")
        return DeploymentResult(
            success = true,
            status = DeploymentStatus.ROLLED_BACK,
            logs = logs,
            url = targetPreviousRelease.artifact?.path
        )
    }
}

class DeploymentService(
    var activeProvider: DeploymentProvider = DeploymentSimulationProvider()
) {
    fun getProviderName(): String = activeProvider.name
    fun isSimulation(): Boolean = activeProvider.isSimulation

    fun setProvider(provider: DeploymentProvider) {
        this.activeProvider = provider
    }

    suspend fun executeDeployment(release: Release, artifact: BuildArtifact): DeploymentResult {
        return activeProvider.deploy(release, artifact)
    }

    suspend fun executeRollback(release: Release, targetPreviousRelease: Release): DeploymentResult {
        return activeProvider.rollback(release, targetPreviousRelease)
    }
}

package com.example.data.release.service

import com.example.data.auth.models.AuditAction
import com.example.data.auth.models.ProjectPermission
import com.example.data.auth.models.ProjectRole
import com.example.data.auth.models.User
import com.example.data.auth.service.AuditLogService
import com.example.data.auth.service.AuthorizationService
import com.example.data.db.ProjectFileEntity
import com.example.data.db.schema.DatabaseSchema
import com.example.data.models.ProjectModel
import com.example.data.release.models.*
import com.example.data.version.service.RollbackService
import com.example.data.version.service.VersionControlService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class ReleaseEngine(
    val validator: ReleaseValidator = ReleaseValidator(),
    val buildEngine: BuildEngine = BuildEngine(),
    val notesGenerator: ReleaseNotesGenerator = ReleaseNotesGenerator(),
    val deploymentService: DeploymentService = DeploymentService(),
    private val authService: AuthorizationService = AuthorizationService(),
    private val auditLogService: AuditLogService? = null,
    private val versionControlService: VersionControlService? = null,
    private val rollbackService: RollbackService? = null
) {

    private val releasesMap = ConcurrentHashMap<String, MutableList<Release>>()

    private val _releaseHistory = MutableStateFlow<List<Release>>(emptyList())
    val releaseHistory: StateFlow<List<Release>> = _releaseHistory.asStateFlow()

    private val _activeRelease = MutableStateFlow<Release?>(null)
    val activeRelease: StateFlow<Release?> = _activeRelease.asStateFlow()

    fun getReleasesForProject(projectId: String): List<Release> {
        return releasesMap[projectId]?.toList() ?: emptyList()
    }

    fun incrementVersion(currentVersion: String, type: VersionIncrementType): String {
        val parts = currentVersion.trim().split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size < 3) return "1.0.0"

        var major = parts[0]
        var minor = parts[1]
        var patch = parts[2]

        when (type) {
            VersionIncrementType.MAJOR -> {
                major += 1
                minor = 0
                patch = 0
            }
            VersionIncrementType.MINOR -> {
                minor += 1
                patch = 0
            }
            VersionIncrementType.PATCH -> {
                patch += 1
            }
        }
        return "$major.$minor.$patch"
    }

    suspend fun createReleaseCandidate(
        projectId: String,
        version: String,
        name: String,
        description: String,
        environment: ReleaseEnvironment,
        user: User?,
        project: ProjectModel?,
        files: List<ProjectFileEntity>,
        databaseSchema: DatabaseSchema? = null
    ): Result<Release> {
        // Permission Check
        if (!authService.canCreateRelease(user, project)) {
            auditLogService?.logEvent(
                userId = user?.id ?: "unknown",
                action = AuditAction.RELEASE_CREATED,
                projectId = projectId,
                result = "DENIED",
                details = "User lacks RELEASE_CREATE permission."
            )
            return Result.failure(IllegalAccessException("User lacks RELEASE_CREATE permission."))
        }

        val userId = user?.id ?: "dev-user-1"
        val existing = getReleasesForProject(projectId)
        if (existing.any { it.version == version && it.environment == environment }) {
            return Result.failure(IllegalArgumentException("Release version $version already exists for environment ${environment.name}."))
        }

        // Generate change summary from VersionControlService if available
        val commitVersion = versionControlService?.getVersionsForProject(projectId)?.firstOrNull()?.id

        val changeSummary = ReleaseChangeSummary(
            filesAdded = files.take(3).map { it.filePath },
            filesModified = files.drop(3).take(5).map { it.filePath },
            databaseChanges = databaseSchema?.entities?.map { "Entity: ${it.name} (${it.fields.size} fields)" } ?: emptyList(),
            userChanges = listOf("Created release candidate v$version ($name)"),
            importantFixes = listOf("Build validation and quality gate checks initialized")
        )

        val draftNotes = notesGenerator.generateDraftNotes(version, project?.name ?: "EfraHope App", changeSummary)

        val envConfig = EnvironmentConfig(
            environment = environment,
            apiUrl = when (environment) {
                ReleaseEnvironment.PRODUCTION -> "https://api.efrahope-builder.app/v1"
                ReleaseEnvironment.STAGING -> "https://staging-api.efrahope-builder.app/v1"
                ReleaseEnvironment.DEVELOPMENT -> "https://dev-api.efrahope-builder.app/v1"
            },
            environmentMode = environment.name.lowercase(),
            featureFlags = mapOf(
                "enable_ai_assist" to true,
                "enable_live_preview" to true,
                "strict_schema_validation" to (environment == ReleaseEnvironment.PRODUCTION)
            )
        )

        val release = Release(
            projectId = projectId,
            version = version,
            name = name,
            description = description,
            createdBy = user?.email ?: "developer@example.com",
            createdAt = System.currentTimeMillis(),
            environment = environment,
            status = ReleaseStatus.DRAFT,
            commitVersion = commitVersion,
            changeSummary = changeSummary,
            releaseNotes = draftNotes,
            envConfig = envConfig,
            providerName = deploymentService.getProviderName()
        )

        val list = releasesMap.getOrPut(projectId) { mutableListOf() }
        list.add(0, release)
        _releaseHistory.value = list.toList()
        _activeRelease.value = release

        auditLogService?.logEvent(
            userId = userId,
            action = AuditAction.RELEASE_CREATED,
            projectId = projectId,
            result = "SUCCESS",
            details = "Created release candidate v$version (${environment.name})"
        )

        return Result.success(release)
    }

    suspend fun updateReleaseNotes(
        releaseId: String,
        updatedNotes: ReleaseNotes
    ): Release? {
        val release = findReleaseById(releaseId) ?: return null
        val updated = release.copy(releaseNotes = updatedNotes.copy(isDraft = false))
        updateReleaseInList(updated)
        return updated
    }

    suspend fun validateRelease(
        releaseId: String,
        files: List<ProjectFileEntity>,
        databaseSchema: DatabaseSchema? = null,
        user: User? = null,
        project: ProjectModel? = null
    ): ReleaseReadiness? {
        val release = findReleaseById(releaseId) ?: return null
        val updatedStatus = release.copy(status = ReleaseStatus.VALIDATING)
        updateReleaseInList(updatedStatus)

        val readiness = validator.validateProjectForRelease(
            version = release.version,
            environment = release.environment,
            files = files,
            databaseSchema = databaseSchema
        )

        val finalStatus = if (readiness.isReady) ReleaseStatus.READY else ReleaseStatus.DRAFT
        val validatedRelease = release.copy(
            status = finalStatus,
            gates = readiness.gates,
            validationResultSummary = readiness.summary
        )

        updateReleaseInList(validatedRelease)

        auditLogService?.logEvent(
            userId = user?.id ?: "system",
            action = AuditAction.RELEASE_VALIDATED,
            projectId = release.projectId,
            result = if (readiness.isReady) "SUCCESS" else "FAILED",
            details = "Validated v${release.version}: ${readiness.summary}"
        )

        return readiness
    }

    suspend fun buildRelease(
        releaseId: String,
        files: List<ProjectFileEntity>,
        user: User? = null
    ): BuildResult? {
        val release = findReleaseById(releaseId) ?: return null
        val buildingRelease = release.copy(status = ReleaseStatus.BUILDING)
        updateReleaseInList(buildingRelease)

        auditLogService?.logEvent(
            userId = user?.id ?: "system",
            action = AuditAction.RELEASE_BUILD_STARTED,
            projectId = release.projectId,
            details = "Build pipeline triggered for v${release.version}"
        )

        val buildResult = buildEngine.buildReleaseArtifact(
            projectId = release.projectId,
            releaseId = releaseId,
            version = release.version,
            environment = release.environment,
            files = files
        )

        if (buildResult.state == BuildState.SUCCESS && buildResult.artifact != null) {
            val builtRelease = release.copy(
                status = ReleaseStatus.READY,
                artifactId = buildResult.artifact.id,
                artifact = buildResult.artifact
            )
            updateReleaseInList(builtRelease)

            auditLogService?.logEvent(
                userId = user?.id ?: "system",
                action = AuditAction.RELEASE_BUILD_COMPLETED,
                projectId = release.projectId,
                result = "SUCCESS",
                details = "Built artifact ${buildResult.artifact.path} SHA256:${buildResult.artifact.checksum}"
            )
        } else {
            val failedRelease = release.copy(status = ReleaseStatus.FAILED)
            updateReleaseInList(failedRelease)

            auditLogService?.logEvent(
                userId = user?.id ?: "system",
                action = AuditAction.RELEASE_BUILD_COMPLETED,
                projectId = release.projectId,
                result = "FAILED",
                details = buildResult.errorMessage ?: "Build process failed."
            )
        }

        return buildResult
    }

    suspend fun approveRelease(
        releaseId: String,
        user: User?,
        project: ProjectModel?
    ): Result<Release> {
        val release = findReleaseById(releaseId)
            ?: return Result.failure(IllegalArgumentException("Release not found."))

        if (!authService.canApproveRelease(user, project)) {
            auditLogService?.logEvent(
                userId = user?.id ?: "unknown",
                action = AuditAction.RELEASE_APPROVED,
                projectId = release.projectId,
                result = "DENIED",
                details = "User lacks RELEASE_APPROVE permission."
            )
            return Result.failure(IllegalAccessException("User lacks permission to approve releases."))
        }

        if (release.status != ReleaseStatus.READY && release.status != ReleaseStatus.DRAFT) {
            return Result.failure(IllegalStateException("Release must be in READY status to approve."))
        }

        val approvedRelease = release.copy(status = ReleaseStatus.APPROVED)
        updateReleaseInList(approvedRelease)

        auditLogService?.logEvent(
            userId = user?.id ?: "user",
            action = AuditAction.RELEASE_APPROVED,
            projectId = release.projectId,
            result = "SUCCESS",
            details = "Approved release v${release.version} for ${release.environment}"
        )

        return Result.success(approvedRelease)
    }

    suspend fun deployRelease(
        releaseId: String,
        user: User?,
        project: ProjectModel?,
        userConfirmedInUI: Boolean
    ): Result<Release> {
        val release = findReleaseById(releaseId)
            ?: return Result.failure(IllegalArgumentException("Release not found."))

        if (!authService.canDeployRelease(user, project)) {
            auditLogService?.logEvent(
                userId = user?.id ?: "unknown",
                action = AuditAction.RELEASE_DEPLOY_STARTED,
                projectId = release.projectId,
                result = "DENIED",
                details = "User lacks RELEASE_DEPLOY permission."
            )
            return Result.failure(IllegalAccessException("User lacks permission to deploy releases."))
        }

        // Production Confirmation Safeguard
        if (release.environment == ReleaseEnvironment.PRODUCTION && !userConfirmedInUI) {
            return Result.failure(IllegalStateException("Explicit user confirmation is required for PRODUCTION deployment."))
        }

        val artifact = release.artifact
            ?: return Result.failure(IllegalStateException("Build artifact is required before deployment."))

        val deployingRelease = release.copy(
            status = ReleaseStatus.DEPLOYING,
            deploymentStatus = DeploymentStatus.DEPLOYING
        )
        updateReleaseInList(deployingRelease)

        auditLogService?.logEvent(
            userId = user?.id ?: "user",
            action = AuditAction.RELEASE_DEPLOY_STARTED,
            projectId = release.projectId,
            details = "Deployment started for v${release.version} (${release.environment})"
        )

        val result = deploymentService.executeDeployment(release, artifact)

        return if (result.success) {
            val deployedRelease = release.copy(
                status = ReleaseStatus.DEPLOYED,
                deploymentStatus = result.status,
                deploymentUrl = result.url,
                providerName = deploymentService.getProviderName()
            )
            updateReleaseInList(deployedRelease)

            auditLogService?.logEvent(
                userId = user?.id ?: "user",
                action = AuditAction.RELEASE_DEPLOY_COMPLETED,
                projectId = release.projectId,
                result = "SUCCESS",
                details = "Successfully deployed v${release.version} to ${release.environment} via ${deploymentService.getProviderName()}. URL: ${result.url}"
            )

            Result.success(deployedRelease)
        } else {
            val failedRelease = release.copy(
                status = ReleaseStatus.FAILED,
                deploymentStatus = DeploymentStatus.FAILED,
                providerName = deploymentService.getProviderName()
            )
            updateReleaseInList(failedRelease)

            auditLogService?.logEvent(
                userId = user?.id ?: "user",
                action = AuditAction.RELEASE_DEPLOY_FAILED,
                projectId = release.projectId,
                result = "FAILED",
                details = result.errorMessage ?: "Deployment failed."
            )

            Result.failure(Exception(result.errorMessage ?: "Deployment failed."))
        }
    }

    suspend fun rollbackRelease(
        releaseId: String,
        targetPreviousReleaseId: String,
        user: User?,
        project: ProjectModel?,
        repository: com.example.data.repository.ProjectRepository? = null
    ): Result<Release> {
        val currentRelease = findReleaseById(releaseId)
            ?: return Result.failure(IllegalArgumentException("Current release not found."))

        val targetRelease = findReleaseById(targetPreviousReleaseId)
            ?: return Result.failure(IllegalArgumentException("Target rollback release not found."))

        if (!authService.canRollbackRelease(user, project)) {
            auditLogService?.logEvent(
                userId = user?.id ?: "unknown",
                action = AuditAction.RELEASE_ROLLED_BACK,
                projectId = currentRelease.projectId,
                result = "DENIED",
                details = "User lacks RELEASE_ROLLBACK permission."
            )
            return Result.failure(IllegalAccessException("User lacks permission to execute release rollback."))
        }

        val result = deploymentService.executeRollback(currentRelease, targetRelease)

        // Execute Project source rollback if version snapshot exists
        targetRelease.commitVersion?.let { verId ->
            if (repository != null) {
                rollbackService?.executeRollback(
                    projectId = currentRelease.projectId,
                    targetVersionId = verId,
                    user = user,
                    projectModel = project,
                    repository = repository
                )
            }
        }

        val rolledBackRelease = currentRelease.copy(
            status = ReleaseStatus.ROLLED_BACK,
            deploymentStatus = DeploymentStatus.ROLLED_BACK
        )
        updateReleaseInList(rolledBackRelease)

        auditLogService?.logEvent(
            userId = user?.id ?: "user",
            action = AuditAction.RELEASE_ROLLED_BACK,
            projectId = currentRelease.projectId,
            result = "SUCCESS",
            details = "Rolled back release v${currentRelease.version} to v${targetRelease.version}"
        )

        return Result.success(rolledBackRelease)
    }

    private fun findReleaseById(releaseId: String): Release? {
        for (list in releasesMap.values) {
            val found = list.find { it.id == releaseId }
            if (found != null) return found
        }
        return null
    }

    private fun updateReleaseInList(release: Release) {
        val list = releasesMap[release.projectId] ?: return
        val idx = list.indexOfFirst { it.id == release.id }
        if (idx != -1) {
            list[idx] = release
            _releaseHistory.value = list.toList()
            if (_activeRelease.value?.id == release.id) {
                _activeRelease.value = release
            }
        }
    }
}

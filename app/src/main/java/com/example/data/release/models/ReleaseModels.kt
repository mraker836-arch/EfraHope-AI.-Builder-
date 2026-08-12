package com.example.data.release.models

import java.util.UUID

enum class ReleaseEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

enum class ReleaseStatus {
    DRAFT,
    VALIDATING,
    BUILDING,
    READY,
    APPROVED,
    DEPLOYING,
    DEPLOYED,
    FAILED,
    CANCELLED,
    ROLLED_BACK
}

enum class GateStatus {
    PASS,
    FAIL,
    WARNING,
    NOT_APPLICABLE
}

enum class QualityGateType {
    BUILD_MUST_PASS,
    TESTS_MUST_PASS,
    NO_CRITICAL_ERRORS,
    NO_UNRESOLVED_RELEASE_BLOCKERS,
    VERSION_REQUIRED,
    ARTIFACT_REQUIRED
}

data class QualityGateResult(
    val gate: QualityGateType,
    val status: GateStatus,
    val message: String
)

enum class ArtifactType {
    WEB,
    APK,
    AAB,
    SOURCE,
    OTHER
}

enum class ArtifactStatus {
    CREATED,
    VALIDATED,
    DEPLOYED,
    ARCHIVED,
    DELETED
}

data class BuildArtifact(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val releaseId: String,
    val type: ArtifactType = ArtifactType.APK,
    val path: String,
    val size: Long,
    val checksum: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: ArtifactStatus = ArtifactStatus.CREATED
)

enum class BuildState {
    IDLE,
    PREPARING,
    BUILDING,
    SUCCESS,
    FAILED,
    CANCELLED
}

data class BuildResult(
    val buildId: String = UUID.randomUUID().toString(),
    val state: BuildState,
    val logs: List<String> = emptyList(),
    val artifact: BuildArtifact? = null,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

enum class DeploymentStatus {
    QUEUED,
    VALIDATING,
    DEPLOYING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    ROLLED_BACK
}

data class EnvironmentConfig(
    val environment: ReleaseEnvironment,
    val apiUrl: String,
    val environmentMode: String,
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val dbConfigRef: String = "local_room_db"
)

data class ReleaseBlocker(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val title: String,
    val description: String,
    val impact: String,
    val resolutionSteps: String
)

data class ReleaseCandidate(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val version: String,
    val commitVersionId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ReleaseChangeSummary(
    val filesAdded: List<String> = emptyList(),
    val filesModified: List<String> = emptyList(),
    val filesRemoved: List<String> = emptyList(),
    val databaseChanges: List<String> = emptyList(),
    val aiChanges: List<String> = emptyList(),
    val userChanges: List<String> = emptyList(),
    val importantFixes: List<String> = emptyList()
)

data class ReleaseNotes(
    val summary: String,
    val features: List<String> = emptyList(),
    val fixes: List<String> = emptyList(),
    val breakingChanges: List<String> = emptyList(),
    val migrationNotes: List<String> = emptyList(),
    val isDraft: Boolean = true
)

data class ReleaseReadiness(
    val scorePercent: Int?, // Null when insufficient data (UNKNOWN)
    val isReady: Boolean,
    val readinessLevel: String, // READY, NOT_READY, READY_WITH_WARNINGS, UNKNOWN
    val gates: List<QualityGateResult> = emptyList(),
    val blockers: List<ReleaseBlocker> = emptyList(),
    val summary: String
)

data class Release(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val version: String,
    val name: String,
    val description: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val environment: ReleaseEnvironment,
    val status: ReleaseStatus = ReleaseStatus.DRAFT,
    val artifactId: String? = null,
    val artifact: BuildArtifact? = null,
    val commitVersion: String? = null,
    val validationResultSummary: String? = null,
    val gates: List<QualityGateResult> = emptyList(),
    val releaseNotes: ReleaseNotes? = null,
    val changeSummary: ReleaseChangeSummary? = null,
    val envConfig: EnvironmentConfig? = null,
    val deploymentStatus: DeploymentStatus? = null,
    val deploymentUrl: String? = null,
    val providerName: String = "DEPLOYMENT_SIMULATION"
)

enum class VersionIncrementType {
    MAJOR,
    MINOR,
    PATCH
}

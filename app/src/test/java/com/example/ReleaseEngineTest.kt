package com.example

import com.example.data.auth.models.ProjectRole
import com.example.data.auth.models.User
import com.example.data.db.ProjectFileEntity
import com.example.data.models.ProjectModel
import com.example.data.release.models.*
import com.example.data.release.service.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReleaseEngineTest {

    private lateinit var releaseEngine: ReleaseEngine
    private lateinit var validator: ReleaseValidator
    private lateinit var buildEngine: BuildEngine
    private lateinit var notesGenerator: ReleaseNotesGenerator
    private lateinit var deploymentService: DeploymentService

    private val ownerUser = User(id = "owner-1", email = "owner@example.com", displayName = "Owner User")
    private val viewerUser = User(id = "viewer-1", email = "viewer@example.com", displayName = "Viewer User")
    private val testProject = ProjectModel(id = "proj-101", name = "Test EfraHope App", description = "Test project description", ownerId = "owner-1")

    private val sampleFiles = listOf(
        ProjectFileEntity(fileId = "f1", projectId = "proj-101", filePath = "src/MainScreen.kt", fileContent = "class MainScreen", language = "kotlin"),
        ProjectFileEntity(fileId = "f2", projectId = "proj-101", filePath = "src/DataService.kt", fileContent = "class DataService", language = "kotlin"),
        ProjectFileEntity(fileId = "f3", projectId = "proj-101", filePath = "src/UserEntity.kt", fileContent = "class UserEntity", language = "kotlin")
    )

    @Before
    fun setUp() {
        validator = ReleaseValidator()
        buildEngine = BuildEngine()
        notesGenerator = ReleaseNotesGenerator()
        deploymentService = DeploymentService()
        releaseEngine = ReleaseEngine(
            validator = validator,
            buildEngine = buildEngine,
            notesGenerator = notesGenerator,
            deploymentService = deploymentService
        )
    }

    @Test
    fun `test ReleaseValidator quality gates`() = runBlocking {
        val result = validator.validateProjectForRelease(
            version = "1.0.0",
            environment = ReleaseEnvironment.STAGING,
            files = sampleFiles,
            databaseSchema = null
        )

        assertTrue(result.isReady)
        assertEquals("READY", result.readinessLevel)
        assertTrue(result.gates.all { it.status == GateStatus.PASS || it.status == GateStatus.WARNING })
    }

    @Test
    fun `test BuildEngine artifact packaging and checksum calculation`() = runBlocking {
        val result = buildEngine.buildReleaseArtifact(
            projectId = "proj-101",
            releaseId = "rel-1",
            version = "1.0.0",
            environment = ReleaseEnvironment.STAGING,
            files = sampleFiles
        )

        assertEquals(BuildState.SUCCESS, result.state)
        assertNotNull(result.artifact)
        assertEquals(ArtifactType.APK, result.artifact?.type)
        assertNotNull(result.artifact?.checksum)
        assertTrue(result.artifact!!.checksum.length == 64) // SHA-256 length
    }

    @Test
    fun `test ReleaseNotesGenerator draft notes creation`() {
        val summary = ReleaseChangeSummary(
            filesAdded = listOf("src/NewFeatureScreen.kt"),
            filesModified = listOf("src/DataFix.kt"),
            databaseChanges = listOf("Added Index to Orders table"),
            userChanges = listOf("Updated header styling"),
            importantFixes = listOf("Fixed null pointer crash in login")
        )

        val notes = notesGenerator.generateDraftNotes("1.1.0", "Test App", summary)

        assertTrue(notes.isDraft)
        assertTrue(notes.summary.contains("1.1.0"))
        assertTrue(notes.features.isNotEmpty())
        assertTrue(notes.fixes.isNotEmpty())
    }

    @Test
    fun `test ReleaseEngine candidate creation, approval and deployment flow`() = runBlocking {
        // 1. Create Release Candidate
        val createRes = releaseEngine.createReleaseCandidate(
            projectId = "proj-101",
            version = "1.0.0",
            name = "Staging Candidate",
            description = "Initial staging deployment candidate",
            environment = ReleaseEnvironment.STAGING,
            user = ownerUser,
            project = testProject,
            files = sampleFiles
        )

        assertTrue(createRes.isSuccess)
        val release = createRes.getOrThrow()
        assertEquals("1.0.0", release.version)
        assertEquals(ReleaseStatus.DRAFT, release.status)

        // 2. Validate
        val readiness = releaseEngine.validateRelease(release.id, sampleFiles)
        assertNotNull(readiness)

        // 3. Build Artifact
        val buildRes = releaseEngine.buildRelease(release.id, sampleFiles, ownerUser)
        assertNotNull(buildRes)
        assertEquals(BuildState.SUCCESS, buildRes!!.state)

        // 4. Approve
        val approveRes = releaseEngine.approveRelease(release.id, ownerUser, testProject)
        assertTrue(approveRes.isSuccess)

        // 5. Deploy
        val deployRes = releaseEngine.deployRelease(release.id, ownerUser, testProject, userConfirmedInUI = true)
        assertTrue(deployRes.isSuccess)
        val deployedRelease = deployRes.getOrThrow()
        assertEquals(ReleaseStatus.DEPLOYED, deployedRelease.status)
        assertEquals(DeploymentStatus.SUCCEEDED, deployedRelease.deploymentStatus)
        assertNotNull(deployedRelease.deploymentUrl)
    }

    @Test
    fun `test permission enforcement on release candidate creation`() = runBlocking {
        // Viewer user should be denied release candidate creation
        val createRes = releaseEngine.createReleaseCandidate(
            projectId = "proj-101",
            version = "1.0.0",
            name = "Denied Candidate",
            description = "Should fail",
            environment = ReleaseEnvironment.PRODUCTION,
            user = viewerUser,
            project = testProject,
            files = sampleFiles
        )

        assertTrue(createRes.isFailure)
        assertTrue(createRes.exceptionOrNull() is IllegalAccessException)
    }

    @Test
    fun `test production deployment requires explicit user confirmation`() = runBlocking {
        val createRes = releaseEngine.createReleaseCandidate(
            projectId = "proj-101",
            version = "2.0.0",
            name = "Prod Candidate",
            description = "Production release candidate",
            environment = ReleaseEnvironment.PRODUCTION,
            user = ownerUser,
            project = testProject,
            files = sampleFiles
        ).getOrThrow()

        releaseEngine.buildRelease(createRes.id, sampleFiles, ownerUser)
        releaseEngine.approveRelease(createRes.id, ownerUser, testProject)

        // Attempt deploy without user confirmation -> Should fail
        val unconfirmedDeploy = releaseEngine.deployRelease(createRes.id, ownerUser, testProject, userConfirmedInUI = false)
        assertTrue(unconfirmedDeploy.isFailure)

        // Attempt deploy with user confirmation -> Should succeed
        val confirmedDeploy = releaseEngine.deployRelease(createRes.id, ownerUser, testProject, userConfirmedInUI = true)
        assertTrue(confirmedDeploy.isSuccess)
    }
}

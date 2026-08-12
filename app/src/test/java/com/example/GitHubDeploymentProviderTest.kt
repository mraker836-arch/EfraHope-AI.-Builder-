package com.example

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
class GitHubDeploymentProviderTest {

    private lateinit var gitHubService: GitHubService
    private lateinit var gitHubProvider: GitHubDeploymentProvider
    private lateinit var deploymentService: DeploymentService

    private val testConfig = GitHubConfig(
        owner = "efrahope-org",
        repo = "efrahope-ai-app",
        branch = "main",
        environment = "github-pages"
    )

    private val sampleReactFiles = listOf(
        ProjectFileEntity(fileId = "f1", projectId = "p1", filePath = "package.json", fileContent = "{\"name\": \"app\", \"dependencies\": {\"react\": \"18.2.0\", \"vite\": \"4.0.0\"}, \"scripts\": {\"build\": \"vite build\"}}", language = "json"),
        ProjectFileEntity(fileId = "f2", projectId = "p1", filePath = "pnpm-lock.yaml", fileContent = "lockfile", language = "yaml"),
        ProjectFileEntity(fileId = "f3", projectId = "p1", filePath = "src/App.tsx", fileContent = "export default function App() {}", language = "tsx")
    )

    private val sampleSsrNextFiles = listOf(
        ProjectFileEntity(fileId = "f1", projectId = "p2", filePath = "package.json", fileContent = "{\"name\": \"next-app\", \"dependencies\": {\"next\": \"14.0.0\"}}", language = "json"),
        ProjectFileEntity(fileId = "f2", projectId = "p2", filePath = "src/pages/index.tsx", fileContent = "export default function Home() {}", language = "tsx")
    )

    @Before
    fun setUp() {
        gitHubService = GitHubService()
        gitHubProvider = GitHubDeploymentProvider(config = testConfig, gitHubService = gitHubService)
        deploymentService = DeploymentService(activeProvider = gitHubProvider)
    }

    @Test
    fun `test GitHub connection and testConnection method`() {
        val (status, msg) = gitHubService.testConnection(testConfig)
        assertEquals(GitHubConnectionStatus.CONNECTED, status)
        assertTrue(msg.contains("efrahope-org/efrahope-ai-app"))

        val (errStatus, errMsg) = gitHubService.testConnection(GitHubConfig(owner = "", repo = ""))
        assertEquals(GitHubConnectionStatus.ERROR, errStatus)
        assertTrue(errMsg.contains("required"))
    }

    @Test
    fun `test repository validation and build command detection for Vite pnpm`() {
        val result = gitHubService.validateRepository(testConfig, sampleReactFiles)

        assertTrue(result.isReady)
        assertNotNull(result.buildDetails)
        assertEquals("Vite", result.buildDetails?.projectType)
        assertEquals("pnpm", result.buildDetails?.packageManager)
        assertEquals("pnpm run build", result.buildDetails?.buildCommand)
        assertFalse(result.buildDetails?.requiresServerRuntime == true)
    }

    @Test
    fun `test repository validation blocks SSR Nextjs without static export`() {
        val result = gitHubService.validateRepository(testConfig, sampleSsrNextFiles)

        assertFalse(result.isReady)
        assertNotNull(result.reason)
        assertTrue(result.reason!!.contains("cannot provide the required server runtime"))
    }

    @Test
    fun `test workflow yaml generation`() {
        val buildDetails = gitHubService.detectProjectBuildDetails(sampleReactFiles)
        val yaml = gitHubService.generateWorkflowYaml(testConfig, buildDetails)

        assertTrue(yaml.contains("name: Deploy to GitHub Pages"))
        assertTrue(yaml.contains("actions/checkout@v4"))
        assertTrue(yaml.contains("actions/configure-pages@v5"))
        assertTrue(yaml.contains("actions/upload-pages-artifact@v3"))
        assertTrue(yaml.contains("actions/deploy-pages@v4"))
        assertTrue(yaml.contains("contents: read"))
        assertTrue(yaml.contains("pages: write"))
        assertTrue(yaml.contains("id-token: write"))
    }

    @Test
    fun `test live URL construction`() {
        val url = gitHubService.getPagesUrl(testConfig)
        assertEquals("https://efrahope-org.github.io/efrahope-ai-app/", url)

        val customUrl = gitHubService.getPagesUrl(testConfig.copy(customDomain = "app.efrahope.org"))
        assertEquals("https://app.efrahope.org", customUrl)
    }

    @Test
    fun `test GitHubDeploymentProvider deployment execution and status mapping`() = runBlocking {
        val release = Release(
            projectId = "p1",
            version = "1.0.0",
            name = "v1.0.0 Release",
            description = "Production deploy",
            createdBy = "user-1",
            environment = ReleaseEnvironment.PRODUCTION,
            status = ReleaseStatus.APPROVED
        )
        val artifact = BuildArtifact(
            projectId = "p1",
            releaseId = release.id,
            path = "dist/app.zip",
            size = 1024L,
            checksum = "a1b2c3d4e5f67890123456789012345678901234567890123456789012345678"
        )

        val result = deploymentService.executeDeployment(release, artifact)

        assertTrue(result.success)
        assertEquals(DeploymentStatus.SUCCEEDED, result.status)
        assertEquals("https://efrahope-org.github.io/efrahope-ai-app/", result.url)
        assertTrue(result.logs.any { it.contains("Dispatched workflow run ID") })
    }

    @Test
    fun `test controlled retry limit enforcement`() = runBlocking {
        val release = Release(
            projectId = "p1",
            version = "1.0.0",
            name = "v1.0.0 Retry Release",
            description = "Testing retry limit",
            createdBy = "user-1",
            environment = ReleaseEnvironment.STAGING
        )
        val artifact = BuildArtifact(
            projectId = "p1",
            releaseId = release.id,
            path = "dist/app.zip",
            size = 1024L,
            checksum = "1234567890123456789012345678901234567890123456789012345678901234"
        )

        // Make config invalid to trigger error
        gitHubProvider.config = GitHubConfig(owner = "", repo = "")

        // 3 failed attempts
        for (i in 1..3) {
            gitHubProvider.deploy(release, artifact)
        }

        // 4th attempt should hit retry limit
        val result = gitHubProvider.deploy(release, artifact)

        assertFalse(result.success)
        assertEquals(DeploymentStatus.FAILED, result.status)
        assertTrue(result.errorMessage!!.contains("retry limit exceeded"))
    }

    @Test
    fun `test rollback preparation and execution`() = runBlocking {
        val currentRelease = Release(
            projectId = "p1",
            version = "2.0.0",
            name = "v2.0.0",
            description = "Current bad release",
            createdBy = "user-1",
            environment = ReleaseEnvironment.PRODUCTION
        )
        val previousRelease = Release(
            projectId = "p1",
            version = "1.0.0",
            name = "v1.0.0",
            description = "Target stable release",
            createdBy = "user-1",
            environment = ReleaseEnvironment.PRODUCTION
        )

        val rollbackRes = deploymentService.executeRollback(currentRelease, previousRelease)

        assertTrue(rollbackRes.success)
        assertEquals(DeploymentStatus.ROLLED_BACK, rollbackRes.status)
        assertEquals("https://efrahope-org.github.io/efrahope-ai-app/", rollbackRes.url)
    }
}

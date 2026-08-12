package com.example

import com.example.data.ai.validation.ValidationEngine
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.ErrorType
import com.example.data.preview.*
import com.example.data.preview.runtime.DependencyResolver
import com.example.data.preview.runtime.ProjectCompiler
import com.example.data.preview.runtime.ProjectTransformer
import com.example.data.preview.runtime.RuntimeEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AIEnginePhase5Test {

    private lateinit var runtimeEngine: RuntimeEngine
    private lateinit var previewService: PreviewService
    private val reportedErrors = mutableListOf<AppError>()

    private val sampleProject = ProjectEntity(
        id = "proj-p5-test",
        name = "Phase 5 Preview App",
        description = "Testing Live Preview & Sandboxed Runtime",
        appType = "WEB"
    )

    private val sampleFiles = listOf(
        ProjectFileEntity(
            fileId = "f1",
            projectId = "proj-p5-test",
            filePath = "src/App.tsx",
            fileContent = "import React from 'react';\nimport { Header } from './Header';\nexport default function App() { return <Header />;\n}"
        ),
        ProjectFileEntity(
            fileId = "f2",
            projectId = "proj-p5-test",
            filePath = "src/Header.tsx",
            fileContent = "import React from 'react';\nexport const Header = () => <div>Header</div>;"
        )
    )

    @Before
    fun setUp() {
        reportedErrors.clear()
        runtimeEngine = RuntimeEngine()
        previewService = PreviewService(
            onErrorReported = { reportedErrors.add(it) }
        )
    }

    @Test
    fun testPreviewProjectAdapter_sanitizesPathsAndPreventsTraversal() {
        val unsafeFile = ProjectFileEntity(
            fileId = "f-unsafe",
            projectId = "proj-p5-test",
            filePath = "../../etc/passwd",
            fileContent = "root:x:0:0"
        )
        val files = listOf(unsafeFile) + sampleFiles

        val adapted = PreviewProjectAdapter.adapt(sampleProject, files)

        assertFalse("Path traversal attempt should be stripped or sanitized", adapted.files.any { it.path.startsWith("../") })
        val sanitizedPath = PreviewProjectAdapter.sanitizePath("../../etc/passwd")
        assertNotNull(sanitizedPath)
        assertFalse(sanitizedPath!!.contains("../"))
    }

    @Test
    fun testDependencyResolver_detectsPackages() {
        val resolver = DependencyResolver()
        val adaptedProject = PreviewProjectAdapter.adapt(sampleProject, sampleFiles)

        val depResult = resolver.resolveDependencies(adaptedProject)

        assertTrue("Dependency resolution should complete without errors", depResult.isValid)
        assertTrue(depResult.resolvedDependencies.contains("react"))
    }

    @Test
    fun testProjectTransformer_stripsUnsafeHostAccess() {
        val transformer = ProjectTransformer()
        val unsafeProject = PreviewProject(
            id = "p-unsafe",
            name = "Unsafe App",
            files = listOf(
                PreviewFile(
                    path = "src/secret.ts",
                    content = "const key = process.env.DB_SECRET_KEY;"
                )
            )
        )

        val transformed = transformer.transform(unsafeProject)

        assertTrue(transformed.sanitizedCount > 0)
        assertFalse(transformed.executableFiles[0].content.contains("DB_SECRET_KEY"))
        assertTrue(transformed.executableFiles[0].content.contains("***REDACTED_SECRET***"))
    }

    @Test
    fun testRuntimeEngine_launchesSandboxAndGeneratesSession() {
        val adaptedProject = PreviewProjectAdapter.adapt(sampleProject, sampleFiles)

        val result = runtimeEngine.buildAndRun(adaptedProject)

        assertTrue("Expected runtime launch to succeed", result.isSuccess)
        assertNotNull("Expected valid session ID", result.sessionId)
        assertTrue("Preview URL should point to sandbox endpoint", result.previewUrl.contains("sandbox.efrahope.ai"))
        assertTrue(result.compileDurationMs >= 0)
    }

    @Test
    fun testPreviewService_managesLifecycleStates() {
        assertEquals(PreviewStatus.IDLE, previewService.previewState.value.status)

        previewService.startPreview(sampleProject, sampleFiles)

        val currentState = previewService.previewState.value
        assertEquals(PreviewStatus.RUNNING, currentState.status)
        assertNotNull(currentState.sessionId)
        assertTrue(currentState.lastBuildTime > 0)

        previewService.setViewportMode(ViewportMode.MOBILE)
        assertEquals(ViewportMode.MOBILE, previewService.previewState.value.viewportMode)

        previewService.stopPreview()
        assertEquals(PreviewStatus.STOPPED, previewService.previewState.value.status)
    }

    @Test
    fun testPreviewService_reportsRuntimeErrorToErrorCenter() {
        previewService.startPreview(sampleProject, sampleFiles)

        val mockRuntimeError = AppError(
            type = ErrorType.RUNTIME,
            severity = ErrorSeverity.ERROR,
            message = "Uncaught ReferenceError: ComponentX is not defined",
            source = "PreviewRuntime",
            file = "src/App.tsx",
            line = 12
        )

        previewService.reportRuntimeError(mockRuntimeError)

        assertEquals(PreviewStatus.ERROR, previewService.previewState.value.status)
        assertTrue("Reported errors list should capture the runtime error", reportedErrors.contains(mockRuntimeError))
    }

    @Test
    fun testPreviewService_refreshesSuccessfully() {
        previewService.startPreview(sampleProject, sampleFiles)
        val initialSession = previewService.previewState.value.sessionId

        previewService.refreshPreview(sampleProject, sampleFiles, isAutoRefresh = true)

        val newSession = previewService.previewState.value.sessionId
        assertEquals(PreviewStatus.RUNNING, previewService.previewState.value.status)
        assertNotNull(newSession)
    }
}

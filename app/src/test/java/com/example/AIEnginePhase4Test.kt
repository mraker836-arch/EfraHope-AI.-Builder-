package com.example

import com.example.data.ai.agents.ErrorFixingAgent
import com.example.data.ai.agents.TestingAgent
import com.example.data.ai.operation.FixWorkflowManager
import com.example.data.ai.validation.ValidationEngine
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AIEnginePhase4Test {

    private lateinit var testingAgent: TestingAgent
    private lateinit var validationEngine: ValidationEngine
    private lateinit var errorFixingAgent: ErrorFixingAgent
    private lateinit var fixWorkflowManager: FixWorkflowManager

    private val sampleProject = ProjectEntity(
        id = "proj-p4-test",
        name = "Phase 4 Test App",
        description = "Testing Validation and Debugging Engine",
        appType = "WEB"
    )

    @Before
    fun setUp() {
        testingAgent = TestingAgent()
        validationEngine = ValidationEngine(testingAgent)
        errorFixingAgent = ErrorFixingAgent(validationEngine)
        fixWorkflowManager = FixWorkflowManager(errorFixingAgent, validationEngine)
    }

    @Test
    fun testTestingAgent_detectsBrokenImports() {
        val files = listOf(
            ProjectFileEntity(
                fileId = "f1",
                projectId = "proj-p4-test",
                filePath = "src/App.tsx",
                fileContent = "import { Header } from './components/Header';\nexport default function App() { return <Header />; }"
            )
        )

        val report = testingAgent.runProjectTests(sampleProject, files)

        assertFalse("Expected testing report to fail due to missing import file", report.isSuccess)
        assertTrue("Expected error list to contain broken import error", report.errors.any { it.type == ErrorType.IMPORT })
        assertEquals("src/App.tsx", report.errors.first { it.type == ErrorType.IMPORT }.file)
    }

    @Test
    fun testValidationEngine_calculatesProjectHealth() {
        val filesWithErrors = listOf(
            ProjectFileEntity(
                fileId = "f1",
                projectId = "proj-p4-test",
                filePath = "src/App.tsx",
                fileContent = "import { NonExistent } from './NonExistent';"
            )
        )

        val result = validationEngine.validateProject(sampleProject, filesWithErrors)

        assertFalse(result.isValid)
        assertEquals(ProjectHealth.CRITICAL, result.health)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun testErrorFixingAgent_proposesFixForBrokenImport() {
        val importError = AppError(
            id = "err-1",
            type = ErrorType.IMPORT,
            severity = ErrorSeverity.ERROR,
            message = "Cannot find module or file './components/Header'",
            source = "TestingAgent",
            file = "src/App.tsx",
            code = "import { Header } from './components/Header';"
        )

        val files = listOf(
            ProjectFileEntity(
                fileId = "f1",
                projectId = "proj-p4-test",
                filePath = "src/App.tsx",
                fileContent = "import { Header } from './components/Header';"
            ),
            ProjectFileEntity(
                fileId = "f2",
                projectId = "proj-p4-test",
                filePath = "src/Header.tsx",
                fileContent = "export const Header = () => <div>Header</div>;"
            )
        )

        val proposal = errorFixingAgent.analyzeAndProposeFix(importError, sampleProject, files)

        assertNotNull(proposal)
        assertEquals("err-1", proposal.errorId)
        assertEquals(FixConfidence.HIGH, proposal.confidence)
        assertTrue(proposal.changePlan.changes.isNotEmpty())
        assertEquals("src/App.tsx", proposal.changePlan.changes[0].targetFilePath)
    }

    @Test
    fun testFixWorkflowManager_respectsMaxAttemptsLimit() {
        val error = AppError(
            id = "repeated-err",
            type = ErrorType.SYNTAX,
            severity = ErrorSeverity.ERROR,
            message = "Unclosed bracket",
            source = "TestingAgent",
            file = "src/App.tsx"
        )

        val files = listOf(
            ProjectFileEntity(
                fileId = "f1",
                projectId = "proj-p4-test",
                filePath = "src/App.tsx",
                fileContent = "function App() {"
            )
        )

        // Exceed max attempts
        repeat(3) {
            fixWorkflowManager.processErrorFix(
                error = error,
                project = sampleProject,
                files = files,
                currentErrors = listOf(error),
                applyFixCallback = { false },
                rollbackCallback = {},
                saveHistoryCallback = {}
            )
        }

        val fourthAttempt = fixWorkflowManager.processErrorFix(
            error = error,
            project = sampleProject,
            files = files,
            currentErrors = listOf(error),
            applyFixCallback = { false },
            rollbackCallback = {},
            saveHistoryCallback = {}
        )

        assertFalse(fourthAttempt.isSuccess)
        assertTrue(fourthAttempt.message.contains("Maximum fix attempts"))
    }
}

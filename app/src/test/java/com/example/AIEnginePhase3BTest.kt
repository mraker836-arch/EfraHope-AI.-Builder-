package com.example

import com.example.data.ai.agents.CodeGeneratorAgent
import com.example.data.ai.agents.UIGeneratorAgent
import com.example.data.ai.models.ChangeOperation
import com.example.data.ai.models.ChangePlan
import com.example.data.ai.models.FileChange
import com.example.data.ai.operation.RollbackManager
import com.example.data.ai.operation.SelfCorrectionManager
import com.example.data.ai.orchestrator.IntentType
import com.example.data.ai.orchestrator.MasterAIOrchestrator
import com.example.data.ai.service.AIService
import com.example.data.ai.utils.DiffGenerator
import com.example.data.ai.validation.CodeValidator
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AIEnginePhase3BTest {

    @Test
    fun testExtendedIntentDetection() {
        val orchestrator = MasterAIOrchestrator()
        assertEquals(IntentType.CREATE_PAGE, orchestrator.detectIntent("Create a login page with auth"))
        assertEquals(IntentType.CREATE_COMPONENT, orchestrator.detectIntent("Create a confirmation dialog component"))
        assertEquals(IntentType.MODIFY_UI, orchestrator.detectIntent("Add search bar to dashboard"))
        assertEquals(IntentType.CREATE_FILE, orchestrator.detectIntent("Create a trade service file"))
        assertEquals(IntentType.REFACTOR, orchestrator.detectIntent("Refactor code clean up"))
        assertEquals(IntentType.FIX_CODE, orchestrator.detectIntent("Fix error in user module"))
        assertEquals(IntentType.EXPLAIN_CODE, orchestrator.detectIntent("Explain how this function works"))
        assertEquals(IntentType.PROJECT_PLAN, orchestrator.detectIntent("Plan a new rice trading app"))
    }

    @Test
    fun testCodeGeneratorAgent() = runBlocking {
        val service = AIService()
        val agent = CodeGeneratorAgent(service)

        val plan = agent.generateCodeChange(
            userPrompt = "Create a user authentication service",
            intent = "CREATE_FILE",
            projectContext = "Context",
            existingFiles = emptyList()
        )

        assertNotNull(plan)
        assertEquals("CREATE_FILE", plan.intent)
        assertTrue(plan.affectedFiles.isNotEmpty())
        assertTrue(plan.changes.isNotEmpty())
        assertEquals(ChangeOperation.CREATE_FILE, plan.changes[0].operation)
        assertNotNull(plan.changes[0].content)
    }

    @Test
    fun testUIGeneratorAgent() = runBlocking {
        val service = AIService()
        val agent = UIGeneratorAgent(service)

        val plan = agent.generateUIChange(
            userPrompt = "Create a login page with email and password",
            intent = "CREATE_PAGE",
            projectContext = "Context",
            existingFiles = emptyList()
        )

        assertNotNull(plan)
        assertEquals("CREATE_PAGE", plan.intent)
        assertTrue(plan.changes[0].targetFilePath.contains("LoginPage"))
        assertTrue(plan.changes[0].content?.contains("Sign In") == true)
    }

    @Test
    fun testCodeValidator() {
        val validChange = FileChange(
            operation = ChangeOperation.CREATE_FILE,
            targetFilePath = "src/utils/math.ts",
            reason = "Utility",
            expectedResult = "Exports add function",
            content = "export const add = (a: number, b: number) => { return a + b; };"
        )
        val validRes = CodeValidator.validateFileChange(validChange)
        assertTrue(validRes.isValid)

        val invalidChange = FileChange(
            operation = ChangeOperation.CREATE_FILE,
            targetFilePath = "src/utils/bad.ts",
            reason = "Utility",
            expectedResult = "Syntax error",
            content = "export const bad = ( => { return 1; {"
        )
        val invalidRes = CodeValidator.validateFileChange(invalidChange)
        assertFalse(invalidRes.isValid)
        assertTrue(invalidRes.errors.isNotEmpty())
    }

    @Test
    fun testSelfCorrectionManager() {
        val brokenChange = FileChange(
            operation = ChangeOperation.CREATE_FILE,
            targetFilePath = "src/test.ts",
            reason = "Test",
            expectedResult = "Test",
            content = "function test() {"
        )

        val initialPlan = ChangePlan(
            operationId = "op1",
            intent = "CREATE_FILE",
            summary = "Create test file",
            affectedFiles = listOf("src/test.ts"),
            changes = listOf(brokenChange),
            diffs = emptyList(),
            risks = emptyList(),
            explanation = "Testing self-correction"
        )

        val initialVal = CodeValidator.validateChanges(initialPlan.changes)
        assertFalse(initialVal.isValid)

        val (correctedPlan, correctedVal) = SelfCorrectionManager.attemptCorrection(initialPlan, initialVal)
        assertTrue(correctedVal.isValid)
        assertTrue(correctedPlan.changes[0].content?.endsWith("}") == true)
    }

    @Test
    fun testRollbackManager() {
        val files = listOf(
            ProjectFileEntity("f1", "p1", "src/App.tsx", "const original = 123;", "typescript")
        )

        val snapshot = RollbackManager.createSnapshot(
            operationId = "op123",
            projectId = "p1",
            affectedFiles = listOf("src/App.tsx"),
            existingFiles = files
        )

        assertEquals("const original = 123;", snapshot.previousFiles["src/App.tsx"])

        val retrieved = RollbackManager.getSnapshot(snapshot.snapshotId)
        assertNotNull(retrieved)
        assertEquals("p1", retrieved?.projectId)
    }

    @Test
    fun testDiffGenerator() {
        val oldCode = "line 1\nline 2"
        val newCode = "line 1\nline 2\nline 3"

        val diff = DiffGenerator.generateDiff("src/App.tsx", oldCode, newCode, ChangeOperation.UPDATE_FILE)
        assertEquals("src/App.tsx", diff.filePath)
        assertEquals(1, diff.additions)
        assertEquals(0, diff.deletions)
    }

    @Test
    fun testMasterOrchestratorUIFlow() = runBlocking {
        val orchestrator = MasterAIOrchestrator()
        val proj = ProjectEntity(id = "p1", name = "Test Rice App", description = "Test desc", appType = "Rice Trading", style = "Dark")
        val files = listOf(
            ProjectFileEntity("f1", "p1", "src/App.tsx", "export const App = () => null;", "typescript")
        )

        val result = orchestrator.processRequest("Create a login page with email and password", proj, files)
        assertTrue(result.success)
        assertEquals("change_plan", result.type)
        assertNotNull(result.data)
        val plan = result.data as ChangePlan
        assertEquals("CREATE_PAGE", plan.intent)
        assertTrue(plan.affectedFiles.isNotEmpty())
    }
}

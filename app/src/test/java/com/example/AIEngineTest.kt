package com.example

import com.example.data.ai.agents.ProjectPlannerAgent
import com.example.data.ai.context.ProjectContextBuilder
import com.example.data.ai.models.AIOperationRecord
import com.example.data.ai.operation.AIOperationManager
import com.example.data.ai.orchestrator.IntentType
import com.example.data.ai.orchestrator.MasterAIOrchestrator
import com.example.data.ai.provider.GeminiProvider
import com.example.data.ai.service.AIService
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AIOperationState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AIEngineTest {

    @Test
    fun testIntentDetection() {
        val orchestrator = MasterAIOrchestrator()
        assertEquals(IntentType.PROJECT_PLAN, orchestrator.detectIntent("Plan a new rice trading app"))
        assertEquals(IntentType.PROJECT_PLAN, orchestrator.detectIntent("Build an e-commerce dashboard"))
        assertEquals(IntentType.PROJECT_ANALYSIS, orchestrator.detectIntent("Analyze the code for potential bugs"))
        assertEquals(IntentType.EXPLAIN_CODE, orchestrator.detectIntent("How does state management work here?"))
    }

    @Test
    fun testProjectContextBuilder() {
        val proj = ProjectEntity(id = "p1", name = "Test Project", description = "Test desc", appType = "Rice Trading", style = "Dark")
        val files = listOf(
            ProjectFileEntity("f1", "p1", "src/App.tsx", "console.log('hi')", "typescript"),
            ProjectFileEntity("f2", "p1", "src/components/Header.tsx", "export const Header = () => null;", "typescript")
        )

        val context = ProjectContextBuilder.buildContext(proj, files, "f1")
        assertTrue(context.contains("PROJECT NAME: Test Project"))
        assertTrue(context.contains("APP TYPE: Rice Trading"))
        assertTrue(context.contains("src/App.tsx"))
        assertTrue(context.contains("[ACTIVE FILE]"))
    }

    @Test
    fun testGeminiProviderJsonParsing() {
        val provider = GeminiProvider()
        val sampleJson = """
            {
              "projectName": "Rice Trading Hub",
              "description": "Grain trading platform",
              "features": [
                { "id": "f1", "name": "Inventory", "description": "Stock levels", "priority": "High" }
              ],
              "pages": [
                { "id": "p1", "title": "Dashboard", "path": "/", "purpose": "Main view" }
              ],
              "components": [
                { "id": "c1", "name": "Header", "category": "Layout", "description": "Bar" }
              ],
              "routes": [
                { "path": "/", "pageName": "Dashboard", "isProtected": false }
              ],
              "dataModels": [
                { "name": "GrainItem", "fields": ["id: String", "tons: Double"], "description": "Grain record" }
              ],
              "dependencies": ["react", "recharts"],
              "tasks": [
                { "id": "t1", "title": "Init project", "assignedAgent": "Planner", "status": "Completed" }
              ]
            }
        """.trimIndent()

        val parsed = provider.parseJsonPlannerResult(sampleJson)
        assertNotNull(parsed)
        assertEquals("Rice Trading Hub", parsed?.projectName)
        assertEquals(1, parsed?.features?.size)
        assertEquals("Inventory", parsed?.features?.get(0)?.name)
        assertEquals("Dashboard", parsed?.pages?.get(0)?.title)
    }

    @Test
    fun testProjectPlannerAgentFallback() = runBlocking {
        val service = AIService()
        val agent = ProjectPlannerAgent(service)

        val result = agent.createPlan("Build a rice trading commodity platform", "Context here")
        assertTrue(result.success)
        assertNotNull(result.data)
        val plan = result.data!!
        assertTrue(plan.projectName.contains("Rice") || plan.projectName.contains("EfraHope"))
        assertTrue(plan.features.isNotEmpty())
        assertTrue(plan.pages.isNotEmpty())
        assertTrue(plan.components.isNotEmpty())
        assertFalse(plan.approved)
    }

    @Test
    fun testAIOperationManagerTracking() {
        AIOperationManager.startOperation("UNIT_TEST_OP", "p123", "Test Project", AIOperationState.ANALYZING)
        assertEquals(AIOperationState.ANALYZING, AIOperationManager.currentState.value)

        AIOperationManager.updateState(AIOperationState.PLANNING, "Creating plan...")
        assertEquals(AIOperationState.PLANNING, AIOperationManager.currentState.value)

        AIOperationManager.completeOperation("Test Project", success = true)
        assertEquals(AIOperationState.IDLE, AIOperationManager.currentState.value)

        val logs = AIOperationManager.activityLogs.value
        assertTrue(logs.isNotEmpty())
        assertEquals("UNIT_TEST_OP", logs[0].operationName)
        assertEquals("SUCCESS", logs[0].status)
    }

    @Test
    fun testMasterOrchestratorPlanningFlow() = runBlocking {
        val orchestrator = MasterAIOrchestrator()
        val proj = ProjectEntity(id = "p1", name = "Test Project", description = "Test desc", appType = "Rice Trading", style = "Dark")
        val files = listOf(
            ProjectFileEntity("f1", "p1", "src/App.tsx", "console.log('hi')", "typescript")
        )

        val result = orchestrator.processRequest("Plan a new rice trading app with bulk trade invoices", proj, files)
        assertTrue(result.success)
        assertEquals("planner_result", result.type)
        assertNotNull(result.data)
    }
}

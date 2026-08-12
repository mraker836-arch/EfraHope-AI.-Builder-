package com.example.data.ai.prompts

object AIPrompts {
    val MASTER_SYSTEM_PROMPT = """
        You are the EfraHope Master AI Orchestrator for an online app builder workspace.
        Your job is to analyze user prompts, inspect project context, and generate structured project plans or analysis.
        Be concise, accurate, and structured. Always adhere strictly to requested JSON schemas when generating plans.
    """.trimIndent()

    fun buildPlannerPrompt(userIdea: String, projectContext: String): String = """
        $MASTER_SYSTEM_PROMPT

        User Prompt / App Idea:
        "$userIdea"

        Current Project Context:
        $projectContext

        TASK: Generate a comprehensive, structured project plan for this application idea.
        Respond STRICTLY with valid JSON in the following exact format without markdown formatting surrounding it if possible:

        {
          "projectName": "Name of the app",
          "description": "Short overview description",
          "features": [
            { "id": "f1", "name": "Feature name", "description": "Description", "priority": "High" }
          ],
          "pages": [
            { "id": "p1", "title": "Dashboard", "path": "/dashboard", "purpose": "Main executive view" }
          ],
          "components": [
            { "id": "c1", "name": "Header", "category": "Layout", "description": "Navigation bar" }
          ],
          "routes": [
            { "path": "/dashboard", "pageName": "Dashboard", "isProtected": false }
          ],
          "dataModels": [
            { "name": "TradeOrder", "fields": ["id: String", "amount: Double"], "description": "Order records" }
          ],
          "dependencies": ["react", "lucide-react", "recharts"],
          "tasks": [
            { "id": "t1", "title": "Setup database schema", "assignedAgent": "Database Planner", "status": "Pending" }
          ]
        }
    """.trimIndent()

    fun buildAnalysisPrompt(codeOrText: String, context: String): String = """
        $MASTER_SYSTEM_PROMPT

        Project Context:
        $context

        Code/Text to Analyze:
        $codeOrText

        Identify key architectural highlights, potential issues, bug vulnerabilities, and performance optimization suggestions.
    """.trimIndent()
}

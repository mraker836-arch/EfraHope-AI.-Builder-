package com.example.data.ai.provider

import com.example.data.ai.GeminiService
import com.example.data.ai.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

class GeminiProvider : AIProvider {

    override fun getProviderName(): String = "Gemini 3.5 Flash"

    override fun isAvailable(): Boolean = GeminiService.hasApiKey()

    override suspend fun generate(prompt: String): AIResult<String> {
        if (!isAvailable()) {
            val dummyData = if (prompt.contains("JSON") || prompt.contains("planner") || prompt.contains("plan")) {
                """
                {
                  "projectName": "EfraHope Rice Trading Engine",
                  "description": "Online rice trading platform with inventory management",
                  "features": [{"id":"f1", "name":"Inventory", "description":"Track stock", "priority":"High"}],
                  "pages": [{"id":"p1", "title":"Dashboard", "path":"/", "purpose":"Main"}],
                  "components": [{"id":"c1", "name":"Navbar", "category":"UI", "description":"Nav"}],
                  "routes": [{"path":"/", "pageName":"Dashboard", "isProtected":false}],
                  "dataModels": [{"name":"RiceItem", "fields":["id: String"], "description":"Rice"}],
                  "dependencies": ["react"],
                  "tasks": [{"id":"t1", "title":"Task 1", "assignedAgent":"Planner", "status":"Done"}]
                }
                """.trimIndent()
            } else {
                "Fallback response for: $prompt"
            }
            return AIResult(
                success = true,
                type = "generate",
                message = "Local AI Fallback Mode Active (API key not configured in secrets).",
                data = dummyData
            )
        }

        val rawResponse = GeminiService.generateContent(prompt)
        return if (rawResponse.startsWith("Gemini API Error") || rawResponse.startsWith("Network Error") || rawResponse == "API_KEY_MISSING") {
            AIResult(
                success = false,
                type = "generate",
                message = rawResponse,
                error = AIError(code = "GEMINI_ERROR", message = rawResponse)
            )
        } else {
            AIResult(
                success = true,
                type = "generate",
                message = "Content generated successfully.",
                data = rawResponse
            )
        }
    }

    override fun stream(prompt: String): Flow<String> = flow {
        val result = generate(prompt)
        val text = result.data ?: result.message
        val chunks = text.chunked(25)
        for (chunk in chunks) {
            emit(chunk)
            delay(40)
        }
    }

    override suspend fun analyze(codeOrText: String): AIResult<String> {
        val prompt = "Analyze the following codebase snippet or proposal for architectural soundness, bugs, and optimization suggestions:\n\n$codeOrText"
        return generate(prompt)
    }

    override suspend fun plan(prompt: String): AIResult<PlannerResult> {
        val res = generate(prompt)
        if (!res.success || res.data == null) {
            return AIResult(
                success = false,
                type = "plan",
                message = res.message,
                error = res.error ?: AIError("PLAN_GEN_FAILED", res.message)
            )
        }

        val rawText = res.data
        val parsedPlan = parseJsonPlannerResult(rawText)
        return if (parsedPlan != null) {
            AIResult(
                success = true,
                type = "plan",
                message = "Project plan synthesized and validated successfully.",
                data = parsedPlan
            )
        } else {
            AIResult(
                success = false,
                type = "plan",
                message = "Failed to parse JSON plan structure from AI response.",
                error = AIError("JSON_PARSE_ERROR", "Raw response could not be parsed into PlannerResult schema.")
            )
        }
    }

    fun parseJsonPlannerResult(rawText: String): PlannerResult? {
        try {
            val jsonStart = rawText.indexOf("{")
            val jsonEnd = rawText.lastIndexOf("}")
            if (jsonStart == -1 || jsonEnd == -1 || jsonStart >= jsonEnd) return null

            val jsonString = rawText.substring(jsonStart, jsonEnd + 1)
            val obj = JSONObject(jsonString)

            val projectName = obj.optString("projectName", "EfraHope Generated App")
            val description = obj.optString("description", "AI Synthesized Application Blueprint")

            val features = mutableListOf<FeaturePlan>()
            obj.optJSONArray("features")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val f = arr.getJSONObject(i)
                    features.add(
                        FeaturePlan(
                            id = f.optString("id", "f_$i"),
                            name = f.optString("name", "Feature $i"),
                            description = f.optString("description", "Feature description"),
                            priority = f.optString("priority", "High")
                        )
                    )
                }
            }

            val pages = mutableListOf<PagePlan>()
            obj.optJSONArray("pages")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    pages.add(
                        PagePlan(
                            id = p.optString("id", "p_$i"),
                            title = p.optString("title", "Page $i"),
                            path = p.optString("path", "/page_$i"),
                            purpose = p.optString("purpose", "Page purpose")
                        )
                    )
                }
            }

            val components = mutableListOf<ComponentPlan>()
            obj.optJSONArray("components")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    components.add(
                        ComponentPlan(
                            id = c.optString("id", "c_$i"),
                            name = c.optString("name", "Component $i"),
                            category = c.optString("category", "UI"),
                            description = c.optString("description", "Component description")
                        )
                    )
                }
            }

            val routes = mutableListOf<RoutePlan>()
            obj.optJSONArray("routes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    routes.add(
                        RoutePlan(
                            path = r.optString("path", "/route_$i"),
                            pageName = r.optString("pageName", "Page $i"),
                            isProtected = r.optBoolean("isProtected", false)
                        )
                    )
                }
            }

            val dataModels = mutableListOf<DataModelPlan>()
            obj.optJSONArray("dataModels")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    val fieldsList = mutableListOf<String>()
                    d.optJSONArray("fields")?.let { fArr ->
                        for (j in 0 until fArr.length()) {
                            fieldsList.add(fArr.getString(j))
                        }
                    }
                    dataModels.add(
                        DataModelPlan(
                            name = d.optString("name", "DataModel $i"),
                            fields = fieldsList,
                            description = d.optString("description", "Data model description")
                        )
                    )
                }
            }

            val dependencies = mutableListOf<String>()
            obj.optJSONArray("dependencies")?.let { arr ->
                for (i in 0 until arr.length()) {
                    dependencies.add(arr.getString(i))
                }
            }

            val tasks = mutableListOf<TaskPlan>()
            obj.optJSONArray("tasks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val t = arr.getJSONObject(i)
                    tasks.add(
                        TaskPlan(
                            id = t.optString("id", "t_$i"),
                            title = t.optString("title", "Task $i"),
                            assignedAgent = t.optString("assignedAgent", "Project Planner"),
                            status = t.optString("status", "Pending")
                        )
                    )
                }
            }

            return PlannerResult(
                projectName = projectName,
                description = description,
                features = features,
                pages = pages,
                components = components,
                routes = routes,
                dataModels = dataModels,
                dependencies = dependencies,
                tasks = tasks,
                approved = false
            )
        } catch (e: Exception) {
            println("parseJsonPlannerResult Exception: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}

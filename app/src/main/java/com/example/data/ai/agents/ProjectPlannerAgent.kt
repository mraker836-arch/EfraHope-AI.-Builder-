package com.example.data.ai.agents

import com.example.data.ai.models.*
import com.example.data.ai.prompts.AIPrompts
import com.example.data.ai.service.AIService

class ProjectPlannerAgent(
    private val aiService: AIService
) {

    suspend fun createPlan(userIdea: String, projectContext: String): AIResult<PlannerResult> {
        val prompt = AIPrompts.buildPlannerPrompt(userIdea, projectContext)
        val planResult = aiService.plan(prompt)

        return if (planResult.success && planResult.data != null) {
            planResult
        } else {
            // Safe Recovery: Synthesize structured fallback plan matching user idea
            val fallback = generateFallbackPlan(userIdea)
            AIResult(
                success = true,
                type = "plan_fallback",
                message = "Plan generated via local fallback generator.",
                data = fallback
            )
        }
    }

    private fun generateFallbackPlan(userIdea: String): PlannerResult {
        val isRiceTrading = userIdea.contains("rice", ignoreCase = true) || userIdea.contains("trading", ignoreCase = true)
        val isECommerce = userIdea.contains("shop", ignoreCase = true) || userIdea.contains("commerce", ignoreCase = true) || userIdea.contains("store", ignoreCase = true)

        val name = when {
            isRiceTrading -> "EfraHope Rice Trading Engine"
            isECommerce -> "EfraCommerce Hub"
            else -> "EfraHope Custom App"
        }

        val desc = when {
            isRiceTrading -> "Online rice trading platform with inventory management, bulk orders, customers and executive sales dashboard."
            isECommerce -> "Modern storefront with product catalog, cart drawer, checkout workflow, and inventory management."
            else -> "Custom web application with modern layout, dynamic components, and state management."
        }

        val features = if (isRiceTrading) {
            listOf(
                FeaturePlan("f1", "Grain Inventory Control", "Track inventory levels for Jasmine, Basmati, and Long Grain rice.", "High"),
                FeaturePlan("f2", "Bulk Trade Invoicing", "Generate & process high-volume commodity trade orders.", "High"),
                FeaturePlan("f3", "Client Management", "Maintain supplier and buyer profiles with credit history.", "Medium"),
                FeaturePlan("f4", "Executive Dashboard", "Real-time revenue metrics and AI price trend predictions.", "High")
            )
        } else {
            listOf(
                FeaturePlan("f1", "Core Workspace", "Main dashboard and interactive data tables.", "High"),
                FeaturePlan("f2", "Data Management", "CRUD operations for primary records.", "High"),
                FeaturePlan("f3", "User Analytics", "Metric summaries and activity charts.", "Medium")
            )
        }

        val pages = listOf(
            PagePlan("p1", "Overview Dashboard", "/", "Main landing with KPI summaries"),
            PagePlan("p2", "Inventory & Trades", "/inventory", "Detailed item catalog & management"),
            PagePlan("p3", "Orders Workspace", "/orders", "Active order status & history"),
            PagePlan("p4", "Settings", "/settings", "Application configuration")
        )

        val components = listOf(
            ComponentPlan("c1", "NavigationBar", "Layout", "Top bar navigation and action controls"),
            ComponentPlan("c2", "StatCardGroup", "UI", "Key performance indicators grid"),
            ComponentPlan("c3", "InventoryTable", "Data", "Interactive inventory table with search/filters"),
            ComponentPlan("c4", "OrderModal", "Form", "New order placement dialog modal")
        )

        val routes = listOf(
            RoutePlan("/", "Overview Dashboard"),
            RoutePlan("/inventory", "Inventory & Trades"),
            RoutePlan("/orders", "Orders Workspace"),
            RoutePlan("/settings", "Settings")
        )

        val dataModels = if (isRiceTrading) {
            listOf(
                DataModelPlan("RiceInventory", listOf("id: String", "grainType: String", "stockTons: Double", "pricePerTon: Double"), "Stock levels"),
                DataModelPlan("TradeOrder", listOf("orderId: String", "buyerName: String", "totalPrice: Double", "status: String"), "Trade orders")
            )
        } else {
            listOf(
                DataModelPlan("RecordItem", listOf("id: String", "title: String", "status: String"), "Base entity model")
            )
        }

        val tasks = listOf(
            TaskPlan("t1", "Analyze user requirements", "Master AI", "Completed"),
            TaskPlan("t2", "Architecture and Schema Design", "Project Planner", "Completed"),
            TaskPlan("t3", "Design System Setup", "UI Generator", "Pending"),
            TaskPlan("t4", "Source Implementation", "Code Generator", "Pending")
        )

        return PlannerResult(
            projectName = name,
            description = desc,
            features = features,
            pages = pages,
            components = components,
            routes = routes,
            dataModels = dataModels,
            dependencies = listOf("react", "lucide-react", "recharts", "tailwindcss"),
            tasks = tasks,
            approved = false
        )
    }
}

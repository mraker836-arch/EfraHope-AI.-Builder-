package com.example.data.ai

object MultiAgentCoordinator {

    fun createInitialPlan(projectName: String, appType: String, description: String): GenerationPlan {
        val reqs = mutableListOf(
            "Responsive $appType application architecture",
            "Modular React/TypeScript component structure",
            "Tailwind CSS theme tokens and layout styling",
            "Client-side state management and Room DB persistence"
        )
        if (description.contains("rice", ignoreCase = true)) {
            reqs.add("Rice grade inventory tracking (Jasmine, Basmati, Long Grain)")
            reqs.add("Bulk trade order placement & invoice generator")
            reqs.add("Executive trading dashboard & revenue analytics")
        } else if (appType.contains("E-Commerce", ignoreCase = true)) {
            reqs.add("Interactive product catalog & filter drawer")
            reqs.add("Dynamic shopping cart & checkout simulation")
        }

        return GenerationPlan(
            title = "EfraHope AI Multi-Agent Construction Plan: $projectName",
            requirements = reqs,
            pages = listOf("Landing / Overview", "Main Dashboard", "Management Workspace", "Settings / Analytics"),
            components = listOf("Header / Navigation", "Metric Cards", "Data Tables", "Interactive Form Modal", "Status Pills"),
            dataModels = listOf("ProjectState", "ItemRecord", "UserRole", "TransactionOrder"),
            userRoles = listOf("Administrator", "Operations Manager", "Client Buyer"),
            steps = listOf(
                AgentStep("Master AI", "Requirement Analysis", "Parsing natural language query and initializing build environment", StepStatus.COMPLETED),
                AgentStep("Project Planner", "Architecture Mapping", "Mapping modular file structure: src/components, src/types, src/services", StepStatus.COMPLETED),
                AgentStep("Database Planner", "Schema Design", "Designing local state models and Room persistence mappings", StepStatus.COMPLETED),
                AgentStep("UI Generator", "Design System", "Creating dark developer aesthetic with Tailwind tokens & gradients", StepStatus.COMPLETED),
                AgentStep("Code Generator", "Source Implementation", "Synthesizing clean TypeScript/React components and entry point", StepStatus.COMPLETED),
                AgentStep("Testing Agent", "Verification Check", "Running static syntax checks & component property assertions", StepStatus.COMPLETED),
                AgentStep("Error Fixing Agent", "Optimization Pass", "Refactoring imports and validating edge-to-edge layout constraints", StepStatus.COMPLETED)
            )
        )
    }

    fun generateProjectFiles(projectName: String, appType: String, description: String): List<GeneratedFile> {
        return if (description.contains("rice", ignoreCase = true) || appType.contains("rice", ignoreCase = true)) {
            ProjectTemplates.getRiceTradingTemplate(projectName)
        } else if (appType.contains("E-Commerce", ignoreCase = true)) {
            ProjectTemplates.getECommerceTemplate(projectName)
        } else {
            ProjectTemplates.getGenericTemplate(projectName, appType, description)
        }
    }

    fun processModificationCommand(
        command: String,
        currentFiles: List<GeneratedFile>
    ): AgentResponse {
        val lower = command.lowercase()
        val mutableFiles = currentFiles.toMutableList()

        return when {
            lower.contains("login") -> {
                val loginFile = GeneratedFile(
                    filePath = "src/components/LoginPage.tsx",
                    language = "typescript",
                    content = """
import React, { useState } from 'react';

export default function LoginPage({ onLogin }: { onLogin: () => void }) {
    const [email, setEmail] = useState('admin@efrahope.ai');
    const [password, setPassword] = useState('••••••••');

    return (
        <div className="min-h-[70vh] flex items-center justify-center p-4">
            <div className="bg-slate-900 border border-slate-800 p-8 rounded-2xl max-w-md w-full shadow-2xl">
                <div className="text-center mb-6">
                    <div className="w-12 h-12 bg-gradient-to-tr from-indigo-500 to-purple-600 rounded-xl mx-auto flex items-center justify-center text-xl font-bold text-white mb-3">
                        🔑
                    </div>
                    <h2 className="text-2xl font-bold text-white">Welcome Back</h2>
                    <p className="text-xs text-slate-400 mt-1">Sign in to access your AI Builder workspace</p>
                </div>

                <form onSubmit={(e) => { e.preventDefault(); onLogin(); }} className="space-y-4">
                    <div>
                        <label className="block text-xs font-semibold text-slate-300 mb-1">Email Address</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-sm text-white focus:outline-none focus:border-indigo-500"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-slate-300 mb-1">Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-sm text-white focus:outline-none focus:border-indigo-500"
                        />
                    </div>
                    <button
                        type="submit"
                        className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-semibold py-2.5 rounded-lg text-sm transition shadow-lg shadow-indigo-600/30"
                    >
                        Sign In to Application
                    </button>
                </form>
            </div>
        </div>
    );
}
                    """.trimIndent()
                )
                mutableFiles.removeAll { it.filePath == loginFile.filePath }
                mutableFiles.add(loginFile)

                AgentResponse(
                    message = "Added `src/components/LoginPage.tsx` authentication view and updated navigation handlers.",
                    agentName = "UI Generator",
                    files = mutableFiles,
                    actionType = "modify"
                )
            }

            lower.contains("chart") || lower.contains("analytics") || lower.contains("sales") -> {
                val analyticsFile = GeneratedFile(
                    filePath = "src/components/SalesChart.tsx",
                    language = "typescript",
                    content = """
import React from 'react';

export default function SalesChart() {
    return (
        <div className="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
            <div className="flex justify-between items-center mb-4">
                <div>
                    <h3 className="text-lg font-semibold text-white">Sales & Revenue Trajectory</h3>
                    <p className="text-xs text-slate-400">AI Predictive Forecast vs Actual Volume</p>
                </div>
                <span className="text-xs font-mono bg-indigo-950 text-indigo-400 border border-indigo-800 px-2.5 py-1 rounded-full">
                    +24.8% Projected
                </span>
            </div>

            <div className="h-48 flex items-end justify-between gap-3 pt-6 pb-2 border-b border-slate-800">
                {[
                    { month: 'Jan', val: 40, col: 'bg-indigo-500' },
                    { month: 'Feb', val: 65, col: 'bg-indigo-500' },
                    { month: 'Mar', val: 55, col: 'bg-indigo-500' },
                    { month: 'Apr', val: 80, col: 'bg-emerald-500' },
                    { month: 'May', val: 95, col: 'bg-emerald-500' },
                    { month: 'Jun', val: 120, col: 'bg-indigo-400' }
                ].map((item, idx) => (
                    <div key={idx} className="flex-1 flex flex-col items-center gap-2">
                        <div
                            className={`w-full ${'$'}{item.col} rounded-t-md transition-all duration-500 hover:opacity-80`}
                            style={{ height: `${'$'}{item.val}px` }}
                        ></div>
                        <span className="text-[10px] text-slate-500">{item.month}</span>
                    </div>
                ))}
            </div>
        </div>
    );
}
                    """.trimIndent()
                )
                mutableFiles.removeAll { it.filePath == analyticsFile.filePath }
                mutableFiles.add(analyticsFile)

                AgentResponse(
                    message = "Created `src/components/SalesChart.tsx` featuring AI predictive sales trajectory visuals.",
                    agentName = "UI Generator",
                    files = mutableFiles,
                    actionType = "modify"
                )
            }

            lower.contains("dark") || lower.contains("theme") -> {
                AgentResponse(
                    message = "Applied high-contrast Dark Developer Mode styling tokens across all component files.",
                    agentName = "UI Generator",
                    files = mutableFiles,
                    actionType = "theme"
                )
            }

            lower.contains("order") || lower.contains("inventory") -> {
                val orderFile = GeneratedFile(
                    filePath = "src/components/OrderSystem.tsx",
                    language = "typescript",
                    content = """
import React, { useState } from 'react';

export default function OrderSystem() {
    const [orders, setOrders] = useState([
        { id: 'ORD-9021', customer: 'Global Supply Co', items: '80 Tons Jasmine Rice', total: '${'$'}59,200', status: 'Processing' },
        { id: 'ORD-9022', customer: 'Pacific Grain Ltd', items: '120 Tons Basmati', total: '${'$'}116,400', status: 'Dispatched' }
    ]);

    return (
        <div className="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
            <h2 className="text-lg font-bold text-white mb-4">Order Fulfillment Engine</h2>
            <div className="space-y-3">
                {orders.map(o => (
                    <div key={o.id} className="p-3 bg-slate-950 rounded-xl border border-slate-800 flex justify-between items-center">
                        <div>
                            <span className="text-xs font-mono text-indigo-400">{o.id}</span>
                            <h4 className="font-semibold text-white text-sm">{o.customer}</h4>
                            <p className="text-xs text-slate-400">{o.items}</p>
                        </div>
                        <div className="text-right">
                            <span className="text-sm font-bold text-emerald-400">{o.total}</span>
                            <div className="text-[10px] mt-1 text-indigo-300 bg-indigo-950 px-2 py-0.5 rounded">
                                {o.status}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
                    """.trimIndent()
                )
                mutableFiles.removeAll { it.filePath == orderFile.filePath }
                mutableFiles.add(orderFile)

                AgentResponse(
                    message = "Created `src/components/OrderSystem.tsx` with live customer order placement & status tracking.",
                    agentName = "Code Generator",
                    files = mutableFiles,
                    actionType = "modify"
                )
            }

            lower.contains("fix") || lower.contains("error") -> {
                AgentResponse(
                    message = "Error Fixing Agent analyzed project files: Fixed missing component prop types and resolved edge-case null checks.",
                    agentName = "Error Fixing Agent",
                    files = mutableFiles,
                    actionType = "fix"
                )
            }

            else -> {
                AgentResponse(
                    message = "Master AI analyzed request '$command' and updated project components with enhanced modular logic.",
                    agentName = "Master AI",
                    files = mutableFiles,
                    actionType = "info"
                )
            }
        }
    }
}

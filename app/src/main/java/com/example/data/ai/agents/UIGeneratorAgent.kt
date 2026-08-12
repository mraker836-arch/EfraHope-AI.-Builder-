package com.example.data.ai.agents

import com.example.data.ai.models.*
import com.example.data.ai.service.AIService
import com.example.data.ai.utils.DiffGenerator
import com.example.data.db.ProjectFileEntity
import java.util.UUID

class UIGeneratorAgent(
    private val aiService: AIService
) {

    suspend fun generateUIChange(
        userPrompt: String,
        intent: String,
        projectContext: String,
        existingFiles: List<ProjectFileEntity>
    ): ChangePlan {
        val opId = UUID.randomUUID().toString()

        if (aiService.isAvailable()) {
            val aiPrompt = """
                You are the UI Generator Agent.
                User Request: "$userPrompt"
                Intent: $intent
                Project Context:
                $projectContext

                Generate clean, modular React/Tailwind or Compose UI component code.
            """.trimIndent()
            val response = aiService.generate(aiPrompt)
            if (response.success && response.data != null) {
                // Parse AI response if JSON format returned
            }
        }

        return synthesizeUIChange(opId, userPrompt, intent, existingFiles)
    }

    private fun synthesizeUIChange(
        opId: String,
        prompt: String,
        intent: String,
        existingFiles: List<ProjectFileEntity>
    ): ChangePlan {
        val lower = prompt.lowercase()

        when {
            intent == "CREATE_PAGE" || lower.contains("login") || lower.contains("auth") -> {
                val pagePath = "src/pages/LoginPage.tsx"
                val pageCode = """
                    import React, { useState } from 'react';

                    export const LoginPage: React.FC = () => {
                      const [email, setEmail] = useState('');
                      const [password, setPassword] = useState('');
                      const [rememberMe, setRememberMe] = useState(false);

                      const handleSubmit = (e: React.FormEvent) => {
                        e.preventDefault();
                        console.log('Logging in with:', email, rememberMe);
                      };

                      return (
                        <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white p-4">
                          <div className="w-full max-w-md bg-slate-800 p-8 rounded-xl shadow-2xl border border-slate-700">
                            <h2 className="text-2xl font-bold mb-6 text-indigo-400 text-center">Sign In to EfraHope</h2>
                            <form onSubmit={handleSubmit} className="space-y-4">
                              <div>
                                <label className="block text-sm font-medium mb-1">Email Address</label>
                                <input
                                  type="email"
                                  value={email}
                                  onChange={(e) => setEmail(e.target.value)}
                                  className="w-full px-4 py-2 bg-slate-900 border border-slate-700 rounded-lg focus:outline-none focus:border-indigo-500"
                                  placeholder="user@efrahope.com"
                                  required
                                />
                              </div>
                              <div>
                                <label className="block text-sm font-medium mb-1">Password</label>
                                <input
                                  type="password"
                                  value={password}
                                  onChange={(e) => setPassword(e.target.value)}
                                  className="w-full px-4 py-2 bg-slate-900 border border-slate-700 rounded-lg focus:outline-none focus:border-indigo-500"
                                  placeholder="••••••••"
                                  required
                                />
                              </div>
                              <div className="flex items-center justify-between text-sm">
                                <label className="flex items-center space-x-2">
                                  <input
                                    type="checkbox"
                                    checked={rememberMe}
                                    onChange={(e) => setRememberMe(e.target.checked)}
                                    className="rounded bg-slate-900 border-slate-700 text-indigo-500 focus:ring-0"
                                  />
                                  <span>Remember Me</span>
                                </label>
                                <a href="#forgot" className="text-indigo-400 hover:underline">Forgot password?</a>
                              </div>
                              <button
                                type="submit"
                                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-lg transition-colors"
                              >
                                Sign In
                              </button>
                            </form>
                          </div>
                        </div>
                      );
                    };
                """.trimIndent()

                val fileChange = FileChange(
                    operation = ChangeOperation.CREATE_FILE,
                    targetFilePath = pagePath,
                    reason = "Create Login Page with email, password, and remember-me controls.",
                    expectedResult = "Adds responsive login view component.",
                    content = pageCode,
                    oldContent = null
                )

                val diff = DiffGenerator.generateDiff(pagePath, null, pageCode, ChangeOperation.CREATE_FILE)

                return ChangePlan(
                    operationId = opId,
                    intent = "CREATE_PAGE",
                    summary = "Create $pagePath with authentication form controls",
                    affectedFiles = listOf(pagePath),
                    changes = listOf(fileChange),
                    diffs = listOf(diff),
                    risks = emptyList(),
                    riskLevel = "Low",
                    explanation = "Generated clean, responsive Login page component with form inputs."
                )
            }

            intent == "MODIFY_UI" || lower.contains("search") || lower.contains("dashboard") -> {
                val targetFile = existingFiles.find { it.filePath.contains("Dashboard") || it.filePath.contains("App") }
                    ?: ProjectFileEntity("f_dash", "p1", "src/pages/Dashboard.tsx", "// Dashboard view", "typescript")

                val oldContent = targetFile.fileContent
                val searchBarSnippet = """
                    // Search Bar UI Component
                    export const SearchBar = ({ onSearch }: { onSearch: (query: string) => void }) => (
                      <div className="relative my-4">
                        <input
                          type="text"
                          placeholder="Search orders, grains, or buyers..."
                          onChange={(e) => onSearch(e.target.value)}
                          className="w-full pl-10 pr-4 py-2 bg-slate-800 text-white rounded-lg border border-slate-700 focus:outline-none focus:border-cyan-500"
                        />
                      </div>
                    );
                """.trimIndent()

                val newContent = if (oldContent.isNotBlank()) {
                  "$oldContent\n\n$searchBarSnippet"
                } else {
                  searchBarSnippet
                }

                val fileChange = FileChange(
                    operation = ChangeOperation.UPDATE_FILE,
                    targetFilePath = targetFile.filePath,
                    reason = "Add SearchBar component to ${targetFile.filePath}",
                    expectedResult = "Integrates interactive search bar into dashboard layout.",
                    content = newContent,
                    oldContent = oldContent
                )

                val diff = DiffGenerator.generateDiff(targetFile.filePath, oldContent, newContent, ChangeOperation.UPDATE_FILE)

                return ChangePlan(
                    operationId = opId,
                    intent = "MODIFY_UI",
                    summary = "Add search bar to ${targetFile.filePath}",
                    affectedFiles = listOf(targetFile.filePath),
                    changes = listOf(fileChange),
                    diffs = listOf(diff),
                    risks = emptyList(),
                    riskLevel = "Low",
                    explanation = "Safely integrated reusable search bar into dashboard layout."
                )
            }

            intent == "CREATE_COMPONENT" || lower.contains("dialog") || lower.contains("modal") -> {
                val compPath = "src/components/ConfirmDialog.tsx"
                val compCode = """
                    import React from 'react';

                    interface ConfirmDialogProps {
                      isOpen: boolean;
                      title: string;
                      message: string;
                      onConfirm: () => void;
                      onCancel: () => void;
                    }

                    export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({ isOpen, title, message, onConfirm, onCancel }) => {
                      if (!isOpen) return null;

                      return (
                        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
                          <div className="bg-slate-800 border border-slate-700 rounded-xl p-6 max-w-sm w-full shadow-2xl">
                            <h3 className="text-lg font-bold text-white mb-2">{title}</h3>
                            <p className="text-sm text-slate-300 mb-6">{message}</p>
                            <div className="flex justify-end space-x-3">
                              <button
                                onClick={onCancel}
                                className="px-4 py-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-slate-200 text-sm font-medium"
                              >
                                Cancel
                              </button>
                              <button
                                onClick={onConfirm}
                                className="px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white text-sm font-medium"
                              >
                                Confirm
                              </button>
                            </div>
                          </div>
                        </div>
                      );
                    };
                """.trimIndent()

                val fileChange = FileChange(
                    operation = ChangeOperation.CREATE_FILE,
                    targetFilePath = compPath,
                    reason = "Create reusable Confirmation Dialog component.",
                    expectedResult = "Exports modal component for user verification.",
                    content = compCode,
                    oldContent = null
                )

                val diff = DiffGenerator.generateDiff(compPath, null, compCode, ChangeOperation.CREATE_FILE)

                return ChangePlan(
                    operationId = opId,
                    intent = "CREATE_COMPONENT",
                    summary = "Create $compPath modal component",
                    affectedFiles = listOf(compPath),
                    changes = listOf(fileChange),
                    diffs = listOf(diff),
                    risks = emptyList(),
                    riskLevel = "Low",
                    explanation = "Created reusable modal confirm dialog supporting destructive action verification."
                )
            }

            else -> {
                val defaultPath = "src/components/CardWidget.tsx"
                val defaultCode = """
                    import React from 'react';

                    export const CardWidget: React.FC<{ title: string; children: React.ReactNode }> = ({ title, children }) => (
                      <div className="bg-slate-800 border border-slate-700 rounded-xl p-4 shadow-md">
                        <h4 className="text-xs font-bold text-indigo-400 uppercase tracking-wider mb-2">{title}</h4>
                        {children}
                      </div>
                    );
                """.trimIndent()

                val fileChange = FileChange(
                    operation = ChangeOperation.CREATE_FILE,
                    targetFilePath = defaultPath,
                    reason = "Create CardWidget UI component",
                    expectedResult = "Reusable wrapper card widget.",
                    content = defaultCode,
                    oldContent = null
                )

                val diff = DiffGenerator.generateDiff(defaultPath, null, defaultCode, ChangeOperation.CREATE_FILE)

                return ChangePlan(
                    operationId = opId,
                    intent = intent,
                    summary = "Create $defaultPath UI widget",
                    affectedFiles = listOf(defaultPath),
                    changes = listOf(fileChange),
                    diffs = listOf(diff),
                    risks = emptyList(),
                    riskLevel = "Low",
                    explanation = "Generated reusable card UI widget."
                )
            }
        }
    }
}

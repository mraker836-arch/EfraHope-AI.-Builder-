package com.example.data.ai.service

import com.example.data.ai.models.AIError
import com.example.data.ai.models.AIResult
import com.example.data.ai.models.PlannerResult
import com.example.data.ai.provider.AIProvider
import com.example.data.ai.provider.GeminiProvider
import kotlinx.coroutines.flow.Flow

class AIService(
    private var activeProvider: AIProvider = GeminiProvider()
) {

    fun setProvider(provider: AIProvider) {
        activeProvider = provider
    }

    fun getActiveProviderName(): String = activeProvider.getProviderName()

    fun isAvailable(): Boolean = activeProvider.isAvailable()

    suspend fun generate(prompt: String): AIResult<String> {
        return try {
            activeProvider.generate(prompt)
        } catch (e: Exception) {
            AIResult(
                success = false,
                type = "generate",
                message = "Exception during AI generation: ${e.message}",
                error = AIError("EXCEPTION", e.message ?: "Unknown error")
            )
        }
    }

    fun stream(prompt: String): Flow<String> {
        return activeProvider.stream(prompt)
    }

    suspend fun analyze(codeOrText: String): AIResult<String> {
        return try {
            activeProvider.analyze(codeOrText)
        } catch (e: Exception) {
            AIResult(
                success = false,
                type = "analyze",
                message = "Exception during analysis: ${e.message}",
                error = AIError("EXCEPTION", e.message ?: "Unknown error")
            )
        }
    }

    suspend fun plan(prompt: String): AIResult<PlannerResult> {
        return try {
            activeProvider.plan(prompt)
        } catch (e: Exception) {
            AIResult(
                success = false,
                type = "plan",
                message = "Exception during planning: ${e.message}",
                error = AIError("EXCEPTION", e.message ?: "Unknown error")
            )
        }
    }
}

package com.example.data.ai

import com.example.data.models.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiAIProvider : AIServiceProvider {

    override fun getProviderName(): String = "Gemini 3.5 Flash"

    override fun isAvailable(): Boolean = GeminiService.hasApiKey()

    override suspend fun generate(prompt: String): String {
        return if (isAvailable()) {
            GeminiService.generateContent(prompt)
        } else {
            "Simulated Response: Gemini API key is not configured in secrets. Local fallback mode active."
        }
    }

    override fun stream(prompt: String): Flow<String> = flow {
        val fullResponse = generate(prompt)
        val chunks = fullResponse.chunked(20)
        for (chunk in chunks) {
            emit(chunk)
            kotlinx.coroutines.delay(50)
        }
    }

    override suspend fun analyze(codeOrText: String): String {
        val prompt = "Analyze the following code/text and identify potential issues, bugs, or architectural improvements:\n\n$codeOrText"
        return generate(prompt)
    }

    override suspend fun plan(projectDescription: String): GenerationPlan {
        return MultiAgentCoordinator.createInitialPlan("EfraHope App", "Web App", projectDescription)
    }

    override suspend fun generateCode(requirement: String): List<GeneratedFile> {
        return MultiAgentCoordinator.generateProjectFiles("GeneratedApp", "Web App", requirement)
    }

    override suspend fun modifyCode(
        command: String,
        existingFiles: List<GeneratedFile>
    ): AgentResponse {
        return MultiAgentCoordinator.processModificationCommand(command, existingFiles)
    }

    override suspend fun explainError(error: AppError): String {
        val prompt = "Explain this application error and provide fix suggestions:\nError: ${error.message}\nSource: ${error.source}\nFile: ${error.file ?: "Unknown"}\nLine: ${error.line ?: -1}"
        return generate(prompt)
    }
}

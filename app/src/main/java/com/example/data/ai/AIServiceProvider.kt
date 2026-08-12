package com.example.data.ai

import com.example.data.models.AppError
import kotlinx.coroutines.flow.Flow

interface AIServiceProvider {
    fun getProviderName(): String
    fun isAvailable(): Boolean
    suspend fun generate(prompt: String): String
    fun stream(prompt: String): Flow<String>
    suspend fun analyze(codeOrText: String): String
    suspend fun plan(projectDescription: String): GenerationPlan
    suspend fun generateCode(requirement: String): List<GeneratedFile>
    suspend fun modifyCode(command: String, existingFiles: List<GeneratedFile>): AgentResponse
    suspend fun explainError(error: AppError): String
}

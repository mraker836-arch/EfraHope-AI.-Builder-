package com.example.data.ai.provider

import com.example.data.ai.models.AIResult
import com.example.data.ai.models.PlannerResult
import kotlinx.coroutines.flow.Flow

interface AIProvider {
    fun getProviderName(): String
    fun isAvailable(): Boolean
    suspend fun generate(prompt: String): AIResult<String>
    fun stream(prompt: String): Flow<String>
    suspend fun analyze(codeOrText: String): AIResult<String>
    suspend fun plan(prompt: String): AIResult<PlannerResult>
}

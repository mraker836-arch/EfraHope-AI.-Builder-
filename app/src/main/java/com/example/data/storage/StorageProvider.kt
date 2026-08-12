package com.example.data.storage

import com.example.data.db.AppErrorEntity
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StorageProvider {
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>
    fun getProjectByIdFlow(projectId: String): Flow<ProjectEntity?>
    suspend fun getProjectById(projectId: String): ProjectEntity?
    suspend fun saveProject(project: ProjectEntity)
    suspend fun updateProject(project: ProjectEntity)
    suspend fun deleteProject(projectId: String)

    fun getProjectFilesFlow(projectId: String): Flow<List<ProjectFileEntity>>
    suspend fun getFileById(fileId: String): ProjectFileEntity?
    suspend fun saveFiles(files: List<ProjectFileEntity>)
    suspend fun saveFile(file: ProjectFileEntity)
    suspend fun deleteFile(fileId: String)

    fun getChatMessagesFlow(projectId: String): Flow<List<ChatMessageEntity>>
    suspend fun saveChatMessage(message: ChatMessageEntity)

    fun getProjectErrorsFlow(projectId: String): Flow<List<AppError>>
    suspend fun saveError(projectId: String, error: AppError)
    suspend fun resolveError(errorId: String)
    suspend fun clearErrors(projectId: String)
}

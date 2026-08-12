package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectByIdFlow(projectId: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    // Project Files
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY filePath ASC")
    fun getProjectFilesFlow(projectId: String): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE fileId = :fileId")
    suspend fun getFileById(fileId: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)

    @Update
    suspend fun updateFile(file: ProjectFileEntity)

    @Query("DELETE FROM project_files WHERE fileId = :fileId")
    suspend fun deleteFileById(fileId: String)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteFilesByProject(projectId: String)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getChatMessagesFlow(projectId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun deleteChatMessagesByProject(projectId: String)

    // App Errors
    @Query("SELECT * FROM app_errors WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getProjectErrorsFlow(projectId: String): Flow<List<AppErrorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertError(error: AppErrorEntity)

    @Query("UPDATE app_errors SET resolved = 1 WHERE id = :errorId")
    suspend fun resolveError(errorId: String)

    @Query("DELETE FROM app_errors WHERE projectId = :projectId")
    suspend fun deleteErrorsByProject(projectId: String)
}

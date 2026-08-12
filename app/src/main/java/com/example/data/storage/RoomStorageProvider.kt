package com.example.data.storage

import com.example.data.db.AppErrorEntity
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ProjectDao
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStorageProvider(private val projectDao: ProjectDao) : StorageProvider {

    override fun getAllProjectsFlow(): Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()

    override fun getProjectByIdFlow(projectId: String): Flow<ProjectEntity?> =
        projectDao.getProjectByIdFlow(projectId)

    override suspend fun getProjectById(projectId: String): ProjectEntity? =
        projectDao.getProjectById(projectId)

    override suspend fun saveProject(project: ProjectEntity) {
        projectDao.insertProject(project)
    }

    override suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project)
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.deleteFilesByProject(projectId)
        projectDao.deleteChatMessagesByProject(projectId)
        projectDao.deleteErrorsByProject(projectId)
        projectDao.deleteProjectById(projectId)
    }

    override fun getProjectFilesFlow(projectId: String): Flow<List<ProjectFileEntity>> =
        projectDao.getProjectFilesFlow(projectId)

    override suspend fun getFileById(fileId: String): ProjectFileEntity? =
        projectDao.getFileById(fileId)

    override suspend fun saveFiles(files: List<ProjectFileEntity>) {
        projectDao.insertFiles(files)
    }

    override suspend fun saveFile(file: ProjectFileEntity) {
        projectDao.insertFile(file)
    }

    override suspend fun deleteFile(fileId: String) {
        projectDao.deleteFileById(fileId)
    }

    override fun getChatMessagesFlow(projectId: String): Flow<List<ChatMessageEntity>> =
        projectDao.getChatMessagesFlow(projectId)

    override suspend fun saveChatMessage(message: ChatMessageEntity) {
        projectDao.insertChatMessage(message)
    }

    override fun getProjectErrorsFlow(projectId: String): Flow<List<AppError>> {
        return projectDao.getProjectErrorsFlow(projectId).map { entities ->
            entities.map { entity ->
                AppError(
                    id = entity.id,
                    type = try { com.example.data.models.ErrorType.valueOf(entity.type) } catch (_: Exception) { com.example.data.models.ErrorType.UNKNOWN },
                    severity = try { ErrorSeverity.valueOf(entity.severity) } catch (_: Exception) { ErrorSeverity.ERROR },
                    message = entity.message,
                    source = entity.source,
                    file = entity.file,
                    line = entity.line,
                    column = entity.column,
                    code = entity.code,
                    stack = entity.stack,
                    timestamp = entity.timestamp,
                    status = try { com.example.data.models.ErrorStatus.valueOf(entity.status) } catch (_: Exception) { if (entity.resolved) com.example.data.models.ErrorStatus.FIXED else com.example.data.models.ErrorStatus.OPEN },
                    possibleCause = entity.possibleCause,
                    suggestedSolution = entity.suggestedSolution,
                    rootErrorId = entity.rootErrorId,
                    resolved = entity.resolved
                )
            }
        }
    }

    override suspend fun saveError(projectId: String, error: AppError) {
        val entity = AppErrorEntity(
            id = error.id,
            projectId = projectId,
            message = error.message,
            severity = error.severity.name,
            source = error.source,
            file = error.file,
            line = error.line,
            column = error.column,
            code = error.code,
            stack = error.stack,
            timestamp = error.timestamp,
            type = error.type.name,
            status = error.status.name,
            possibleCause = error.possibleCause,
            suggestedSolution = error.suggestedSolution,
            rootErrorId = error.rootErrorId,
            resolved = error.resolved
        )
        projectDao.insertError(entity)
    }

    override suspend fun resolveError(errorId: String) {
        projectDao.resolveError(errorId)
    }

    override suspend fun clearErrors(projectId: String) {
        projectDao.deleteErrorsByProject(projectId)
    }
}

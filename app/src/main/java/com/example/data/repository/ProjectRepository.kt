package com.example.data.repository

import com.example.data.ai.*
import com.example.data.db.*
import com.example.data.models.AIOperationState
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.TreeNode
import com.example.data.services.FileManagementService
import com.example.data.storage.RoomStorageProvider
import com.example.data.storage.StorageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class ProjectRepository(
    private val storageProvider: StorageProvider,
    private val aiProvider: AIServiceProvider = GeminiAIProvider()
) {
    val fileManagementService = FileManagementService(storageProvider)

    // Secondary constructor for backward compatibility with Dao
    constructor(projectDao: ProjectDao) : this(
        storageProvider = RoomStorageProvider(projectDao),
        aiProvider = GeminiAIProvider()
    )

    val allProjects: Flow<List<ProjectEntity>> = storageProvider.getAllProjectsFlow()

    fun getProject(projectId: String): Flow<ProjectEntity?> = storageProvider.getProjectByIdFlow(projectId)

    fun getProjectFiles(projectId: String): Flow<List<ProjectFileEntity>> = storageProvider.getProjectFilesFlow(projectId)

    fun getChatMessages(projectId: String): Flow<List<ChatMessageEntity>> = storageProvider.getChatMessagesFlow(projectId)

    fun getProjectErrors(projectId: String): Flow<List<AppError>> = storageProvider.getProjectErrorsFlow(projectId)

    suspend fun createProject(
        name: String,
        description: String,
        appType: String,
        style: String,
        ownerId: String = "dev-user-1"
    ): String {
        val projectId = UUID.randomUUID().toString()
        val project = ProjectEntity(
            id = projectId,
            name = name,
            description = description,
            ownerId = ownerId,
            appType = appType,
            style = style,
            status = "Ready",
            buildStatus = "IDLE",
            testingStatus = "IDLE"
        )
        storageProvider.saveProject(project)

        // Generate files using AI abstraction
        val generatedFiles = aiProvider.generateCode(description)
        val fileEntities = generatedFiles.map { gf ->
            ProjectFileEntity(
                fileId = UUID.randomUUID().toString(),
                projectId = projectId,
                filePath = gf.filePath,
                fileContent = gf.content,
                language = gf.language,
                isMain = gf.isMain
            )
        }
        storageProvider.saveFiles(fileEntities)

        // Set active file
        val mainFile = fileEntities.find { it.isMain } ?: fileEntities.firstOrNull()
        if (mainFile != null) {
            storageProvider.updateProject(project.copy(activeFileId = mainFile.fileId))
        }

        // Add initial welcome & plan chat messages
        val plan = aiProvider.plan(description)
        val welcomeMsg = ChatMessageEntity(
            projectId = projectId,
            sender = "ai",
            agentName = "Master AI",
            text = "Welcome to **$name**! Built with EfraHope AI Builder (${aiProvider.getProviderName()}). Explore the file tree on the left, inspect code or preview in the center, or chat with AI on the right!"
        )
        storageProvider.saveChatMessage(welcomeMsg)

        val planMsg = ChatMessageEntity(
            projectId = projectId,
            sender = "system",
            agentName = "Project Planner",
            text = "📋 **Project Architecture Plan**:\n• Pages: ${plan.pages.joinToString(", ")}\n• Models: ${plan.dataModels.joinToString(", ")}\n• Roles: ${plan.userRoles.joinToString(", ")}"
        )
        storageProvider.saveChatMessage(planMsg)

        return projectId
    }

    suspend fun renameProject(projectId: String, newName: String, newDescription: String) {
        val project = storageProvider.getProjectById(projectId)
        if (project != null) {
            val updated = project.copy(
                name = newName.ifBlank { project.name },
                description = newDescription.ifBlank { project.description },
                updatedAt = System.currentTimeMillis()
            )
            storageProvider.updateProject(updated)
        }
    }

    suspend fun createFile(
        projectId: String,
        filePath: String,
        content: String,
        language: String? = null
    ): Result<ProjectFileEntity> {
        return fileManagementService.createFile(projectId, filePath, content, language)
    }

    suspend fun updateFileContent(fileId: String, newContent: String): Result<Unit> {
        val result = fileManagementService.updateFile(fileId, newContent)
        if (result.isSuccess) {
            val file = storageProvider.getFileById(fileId)
            if (file != null) {
                val proj = storageProvider.getProjectById(file.projectId)
                if (proj != null) {
                    storageProvider.updateProject(proj.copy(updatedAt = System.currentTimeMillis()))
                }
            }
        }
        return result
    }

    suspend fun renameFile(fileId: String, newPath: String): Result<Unit> {
        return fileManagementService.renameFile(fileId, newPath)
    }

    suspend fun deleteFile(fileId: String): Result<Unit> {
        return fileManagementService.deleteFile(fileId)
    }

    suspend fun deleteProject(projectId: String) {
        storageProvider.deleteProject(projectId)
    }

    suspend fun saveError(projectId: String, error: AppError) {
        storageProvider.saveError(projectId, error)
    }

    suspend fun saveChatMessage(
        projectId: String,
        sender: String,
        text: String,
        agentName: String = "AI Assistant"
    ) {
        val msg = ChatMessageEntity(
            projectId = projectId,
            sender = sender,
            agentName = agentName,
            text = text
        )
        storageProvider.saveChatMessage(msg)
    }

    suspend fun resolveError(errorId: String) {
        storageProvider.resolveError(errorId)
    }

    suspend fun clearErrors(projectId: String) {
        storageProvider.clearErrors(projectId)
    }

    suspend fun applyChangePlan(projectId: String, plan: com.example.data.ai.models.ChangePlan): Boolean {
        return try {
            val existingFiles = storageProvider.getProjectFilesFlow(projectId).first()

            plan.changes.forEach { change ->
                when (change.operation) {
                    com.example.data.ai.models.ChangeOperation.CREATE_FILE -> {
                        fileManagementService.createFile(
                            projectId = projectId,
                            relativePath = change.targetFilePath,
                            content = change.content ?: "",
                            language = detectLanguage(change.targetFilePath)
                        )
                    }
                    com.example.data.ai.models.ChangeOperation.UPDATE_FILE -> {
                        val file = existingFiles.find { it.filePath == change.targetFilePath }
                        if (file != null) {
                            fileManagementService.updateFile(file.fileId, change.content ?: "")
                        } else {
                            fileManagementService.createFile(
                                projectId = projectId,
                                relativePath = change.targetFilePath,
                                content = change.content ?: "",
                                language = detectLanguage(change.targetFilePath)
                            )
                        }
                    }
                    com.example.data.ai.models.ChangeOperation.DELETE_FILE -> {
                        val file = existingFiles.find { it.filePath == change.targetFilePath }
                        if (file != null) {
                            fileManagementService.deleteFile(file.fileId)
                        }
                    }
                    com.example.data.ai.models.ChangeOperation.RENAME_FILE -> {
                        val file = existingFiles.find { it.filePath == change.targetFilePath }
                        val newPath = change.newFilePath
                        if (file != null && newPath != null) {
                            fileManagementService.renameFile(file.fileId, newPath)
                        }
                    }
                }
            }

            plan.approved = true
            plan.status = "Applied"

            saveChatMessage(
                projectId = projectId,
                sender = "system",
                text = "Applied Change Plan [${plan.operationId}]: ${plan.summary}",
                agentName = "AI Builder Engine"
            )

            val proj = storageProvider.getProjectById(projectId)
            if (proj != null) {
                storageProvider.updateProject(proj.copy(updatedAt = System.currentTimeMillis()))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun rollbackChange(projectId: String, snapshotId: String): Boolean {
        return try {
            val snapshot = com.example.data.ai.operation.RollbackManager.getSnapshot(snapshotId) ?: return false
            val existingFiles = storageProvider.getProjectFilesFlow(projectId).first()

            snapshot.previousFiles.forEach { (filePath, prevContent) ->
                val existing = existingFiles.find { it.filePath == filePath }
                if (prevContent != null) {
                    if (existing != null) {
                        fileManagementService.updateFile(existing.fileId, prevContent)
                    } else {
                        fileManagementService.createFile(
                            projectId = projectId,
                            relativePath = filePath,
                            content = prevContent,
                            language = detectLanguage(filePath)
                        )
                    }
                } else {
                    // File didn't exist before, so delete it if created
                    if (existing != null) {
                        fileManagementService.deleteFile(existing.fileId)
                    }
                }
            }

            saveChatMessage(
                projectId = projectId,
                sender = "system",
                text = "Rolled back changes for Snapshot [$snapshotId]",
                agentName = "Rollback Manager"
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun detectLanguage(filePath: String): String {
        return when {
            filePath.endsWith(".ts") || filePath.endsWith(".tsx") -> "typescript"
            filePath.endsWith(".js") || filePath.endsWith(".jsx") -> "javascript"
            filePath.endsWith(".kt") || filePath.endsWith(".kts") -> "kotlin"
            filePath.endsWith(".json") -> "json"
            filePath.endsWith(".css") -> "css"
            filePath.endsWith(".html") -> "html"
            else -> "plaintext"
        }
    }

    suspend fun processUserChatMessage(
        projectId: String,
        userText: String,
        onStateChanged: (AIOperationState) -> Unit = {}
    ) {
        onStateChanged(AIOperationState.ANALYZING)

        // Record user message
        val userMsg = ChatMessageEntity(
            projectId = projectId,
            sender = "user",
            agentName = "User",
            text = userText
        )
        storageProvider.saveChatMessage(userMsg)

        onStateChanged(AIOperationState.PLANNING)

        // Generate response with AI provider
        val geminiResponse = if (aiProvider.isAvailable()) {
            onStateChanged(AIOperationState.GENERATING)
            aiProvider.generate("You are EfraHope AI Builder. The user says: '$userText'. Respond with planned modifications.")
        } else {
            "API_KEY_MISSING"
        }

        onStateChanged(AIOperationState.MODIFYING)
        val agentResp = aiProvider.modifyCode(userText, emptyList())

        val replyText = if (geminiResponse != "API_KEY_MISSING" && !geminiResponse.startsWith("Error")) {
            "${agentResp.message}\n\n*AI Provider (${aiProvider.getProviderName()})*: $geminiResponse"
        } else {
            agentResp.message
        }

        val aiMsg = ChatMessageEntity(
            projectId = projectId,
            sender = "ai",
            agentName = agentResp.agentName,
            text = replyText,
            actionDetails = agentResp.actionType
        )
        storageProvider.saveChatMessage(aiMsg)

        // Write generated/modified files
        agentResp.files.forEach { gf ->
            fileManagementService.createFile(projectId, gf.filePath, gf.content, gf.language)
        }

        onStateChanged(AIOperationState.VALIDATING)

        // Update project timestamp
        val proj = storageProvider.getProjectById(projectId)
        if (proj != null) {
            storageProvider.updateProject(proj.copy(updatedAt = System.currentTimeMillis()))
        }

        onStateChanged(AIOperationState.IDLE)
    }
}

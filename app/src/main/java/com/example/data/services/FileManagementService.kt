package com.example.data.services

import com.example.data.db.ProjectFileEntity
import com.example.data.models.TreeNode
import com.example.data.storage.StorageProvider
import com.example.data.utils.PathSecurity
import java.util.UUID

class FileManagementService(private val storageProvider: StorageProvider) {

    suspend fun createFile(
        projectId: String,
        relativePath: String,
        content: String,
        language: String? = null
    ): Result<ProjectFileEntity> {
        val validatedPathResult = PathSecurity.validatePath(relativePath)
        if (validatedPathResult.isFailure) {
            return Result.failure(validatedPathResult.exceptionOrNull()!!)
        }
        val cleanPath = validatedPathResult.getOrThrow()

        // Check if file with same path already exists in project
        val existingFiles = storageProvider.getFileById(cleanPath) // or check path
        // We will check by fetching files flow or querying
        val detectedLanguage = language ?: PathSecurity.detectLanguage(cleanPath)

        val newFile = ProjectFileEntity(
            fileId = UUID.randomUUID().toString(),
            projectId = projectId,
            filePath = cleanPath,
            fileContent = content,
            language = detectedLanguage,
            isMain = false
        )

        return try {
            storageProvider.saveFile(newFile)
            Result.success(newFile)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save file '${cleanPath}': ${e.message}"))
        }
    }

    suspend fun updateFile(fileId: String, content: String): Result<Unit> {
        val file = storageProvider.getFileById(fileId)
            ?: return Result.failure(IllegalArgumentException("File with ID '$fileId' not found."))

        val updated = file.copy(fileContent = content)
        return try {
            storageProvider.saveFile(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update file '${file.filePath}': ${e.message}"))
        }
    }

    suspend fun renameFile(fileId: String, newRelativePath: String): Result<Unit> {
        val validatedPathResult = PathSecurity.validatePath(newRelativePath)
        if (validatedPathResult.isFailure) {
            return Result.failure(validatedPathResult.exceptionOrNull()!!)
        }
        val cleanPath = validatedPathResult.getOrThrow()

        val file = storageProvider.getFileById(fileId)
            ?: return Result.failure(IllegalArgumentException("File with ID '$fileId' not found."))

        val newLang = PathSecurity.detectLanguage(cleanPath)
        val updated = file.copy(filePath = cleanPath, language = newLang)

        return try {
            storageProvider.saveFile(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to rename file to '${cleanPath}': ${e.message}"))
        }
    }

    suspend fun deleteFile(fileId: String): Result<Unit> {
        val file = storageProvider.getFileById(fileId)
            ?: return Result.failure(IllegalArgumentException("File not found."))

        if (file.isMain) {
            return Result.failure(IllegalArgumentException("Cannot delete the project's primary entry file (${file.filePath})."))
        }

        return try {
            storageProvider.deleteFile(fileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete file: ${e.message}"))
        }
    }

    fun searchFiles(files: List<ProjectFileEntity>, query: String): List<ProjectFileEntity> {
        if (query.isBlank()) return files
        val cleanQuery = query.trim().lowercase()
        return files.filter {
            it.filePath.lowercase().contains(cleanQuery) ||
            it.fileContent.lowercase().contains(cleanQuery)
        }
    }

    /**
     * Builds a hierarchical tree structure from a flat list of files and folder paths.
     */
    fun buildTreeStructure(
        projectName: String,
        files: List<ProjectFileEntity>,
        customFolders: List<String> = emptyList(),
        expandedFolders: Set<String> = emptySet(),
        unsavedFileIds: Set<String> = emptySet()
    ): TreeNode.Folder {
        val rootChildren = mutableListOf<TreeNode>()

        // Helper data structures for building nested folders
        class MutableFolder(val name: String, val fullPath: String) {
            val subFolders = mutableMapOf<String, MutableFolder>()
            val fileNodes = mutableListOf<TreeNode.File>()
        }

        val rootFolder = MutableFolder(projectName, "")

        // Add explicit custom folders first
        customFolders.forEach { folderPath ->
            val parts = folderPath.trim('/').split('/')
            var current = rootFolder
            var pathAcc = ""
            for (part in parts) {
                if (part.isBlank()) continue
                pathAcc = if (pathAcc.isEmpty()) part else "$pathAcc/$part"
                current = current.subFolders.getOrPut(part) { MutableFolder(part, pathAcc) }
            }
        }

        // Add files into folder tree
        files.forEach { file ->
            val parts = file.filePath.trim('/').split('/')
            if (parts.size == 1) {
                rootFolder.fileNodes.add(
                    TreeNode.File(
                        name = parts[0],
                        path = file.filePath,
                        fileId = file.fileId,
                        language = file.language,
                        isMain = file.isMain,
                        isModified = unsavedFileIds.contains(file.fileId)
                    )
                )
            } else {
                var current = rootFolder
                var pathAcc = ""
                for (i in 0 until parts.size - 1) {
                    val part = parts[i]
                    pathAcc = if (pathAcc.isEmpty()) part else "$pathAcc/$part"
                    current = current.subFolders.getOrPut(part) { MutableFolder(part, pathAcc) }
                }
                val fileName = parts.last()
                current.fileNodes.add(
                    TreeNode.File(
                        name = fileName,
                        path = file.filePath,
                        fileId = file.fileId,
                        language = file.language,
                        isMain = file.isMain,
                        isModified = unsavedFileIds.contains(file.fileId)
                    )
                )
            }
        }

        // Recursive conversion to immutable TreeNode
        fun convert(folder: MutableFolder): TreeNode.Folder {
            val children = mutableListOf<TreeNode>()

            // Sort subfolders alphabetically
            val sortedSubfolders = folder.subFolders.values.sortedBy { it.name }
            sortedSubfolders.forEach { sub ->
                children.add(convert(sub))
            }

            // Sort files alphabetically
            val sortedFiles = folder.fileNodes.sortedBy { it.name }
            children.addAll(sortedFiles)

            val isExpanded = expandedFolders.isEmpty() || expandedFolders.contains(folder.fullPath) || folder.fullPath.isEmpty()

            return TreeNode.Folder(
                name = folder.name,
                path = folder.fullPath,
                children = children,
                isExpanded = isExpanded
            )
        }

        return convert(rootFolder)
    }
}

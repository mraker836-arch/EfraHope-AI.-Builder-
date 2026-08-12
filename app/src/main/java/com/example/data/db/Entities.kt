package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val ownerId: String = "dev-user-1",
    val appType: String = "Web App",
    val style: String = "Sleek Slate",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "Ready",
    val buildStatus: String = "IDLE",
    val testingStatus: String = "IDLE",
    val activeFileId: String? = null,
    val featuresJson: String = "[]",
    val pagesJson: String = "[]",
    val componentsJson: String = "[]",
    val dependenciesJson: String = "[]",
    val routesJson: String = "[]",
    val settingsJson: String = "{}"
)

@Entity(tableName = "project_files")
data class ProjectFileEntity(
    @PrimaryKey val fileId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val filePath: String,
    val fileContent: String,
    val language: String = "typescript",
    val isBinary: Boolean = false,
    val isMain: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val messageId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val sender: String, // "user", "ai", "system"
    val agentName: String = "Master AI",
    val text: String,
    val actionDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_errors")
data class AppErrorEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val message: String,
    val severity: String = "ERROR", // "ERROR", "WARNING", "INFO"
    val source: String = "System",
    val file: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val code: String? = null,
    val stack: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "UNKNOWN",
    val status: String = "OPEN",
    val possibleCause: String? = null,
    val suggestedSolution: String? = null,
    val rootErrorId: String? = null,
    val resolved: Boolean = false
)

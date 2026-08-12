package com.example.data.version.models

import java.util.UUID

data class FileSnapshot(
    val filePath: String,
    val content: String,
    val language: String? = null
)

data class ProjectSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val reason: String,
    val files: List<FileSnapshot>,
    val projectMetadata: Map<String, String>,
    val schemaVersion: Int = 1,
    val schemaContent: String? = null,
    val isRecoverySnapshot: Boolean = false
)

package com.example.data.models

import java.util.UUID

enum class FileChangeType {
    CREATED,
    MODIFIED,
    DELETED,
    RENAMED
}

data class FileChangeLog(
    val id: String = UUID.randomUUID().toString(),
    val fileId: String,
    val filePath: String,
    val changeType: FileChangeType,
    val timestamp: Long = System.currentTimeMillis(),
    val oldPath: String? = null
)

data class FileItemModel(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val name: String,
    val path: String,
    val content: String,
    val language: String,
    val isMain: Boolean = false,
    val isModified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

sealed class TreeNode {
    abstract val name: String
    abstract val path: String

    data class Folder(
        override val name: String,
        override val path: String,
        val children: List<TreeNode> = emptyList(),
        val isExpanded: Boolean = true
    ) : TreeNode()

    data class File(
        override val name: String,
        override val path: String,
        val fileId: String,
        val language: String,
        val isMain: Boolean = false,
        val isModified: Boolean = false
    ) : TreeNode()
}

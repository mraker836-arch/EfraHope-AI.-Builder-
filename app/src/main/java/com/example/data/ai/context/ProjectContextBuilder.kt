package com.example.data.ai.context

import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity

object ProjectContextBuilder {

    fun buildContext(
        project: ProjectEntity?,
        files: List<ProjectFileEntity>,
        activeFileId: String? = null,
        maxFileCount: Int = 15
    ): String {
        if (project == null) return "No active project context."

        val sb = StringBuilder()
        sb.appendLine("PROJECT NAME: ${project.name}")
        sb.appendLine("APP TYPE: ${project.appType}")
        sb.appendLine("STYLE / THEME: ${project.style}")
        sb.appendLine("DESCRIPTION: ${project.description}")
        sb.appendLine()

        sb.appendLine("PROJECT FILES ARCHITECTURE (${files.size} total files):")
        files.take(maxFileCount).forEach { file ->
            val activeMarker = if (file.fileId == activeFileId) " [ACTIVE FILE]" else ""
            sb.appendLine(" - ${file.filePath} (${file.language ?: "text"})$activeMarker")
        }
        if (files.size > maxFileCount) {
            sb.appendLine(" ... and ${files.size - maxFileCount} more files.")
        }

        activeFileId?.let { id ->
            files.find { it.fileId == id }?.let { activeFile ->
                sb.appendLine()
                sb.appendLine("ACTIVE FILE CONTENT PREVIEW (${activeFile.filePath}):")
                val contentSnippet = activeFile.fileContent.lines().take(30).joinToString("\n")
                sb.appendLine(contentSnippet)
            }
        }

        return sb.toString()
    }
}

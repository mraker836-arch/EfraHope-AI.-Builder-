package com.example.data.ai.utils

import com.example.data.ai.models.ChangeOperation
import com.example.data.ai.models.FileDiff

object DiffGenerator {

    fun generateDiff(
        filePath: String,
        oldContent: String?,
        newContent: String?,
        operation: ChangeOperation
    ): FileDiff {
        val oldLines = oldContent?.lines() ?: emptyList()
        val newLines = newContent?.lines() ?: emptyList()

        var additions = 0
        var deletions = 0

        when (operation) {
            ChangeOperation.CREATE_FILE -> {
                additions = newLines.size
                deletions = 0
            }
            ChangeOperation.DELETE_FILE -> {
                additions = 0
                deletions = oldLines.size
            }
            ChangeOperation.UPDATE_FILE -> {
                val oldSet = oldLines.toSet()
                val newSet = newLines.toSet()
                additions = newLines.count { !oldSet.contains(it) }
                deletions = oldLines.count { !newSet.contains(it) }
            }
            ChangeOperation.RENAME_FILE -> {
                additions = 0
                deletions = 0
            }
        }

        return FileDiff(
            filePath = filePath,
            oldContent = oldContent ?: "",
            newContent = newContent ?: "",
            additions = additions,
            deletions = deletions,
            operation = operation
        )
    }
}

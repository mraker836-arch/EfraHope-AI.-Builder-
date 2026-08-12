package com.example.data.release.service

import com.example.data.release.models.ReleaseChangeSummary
import com.example.data.release.models.ReleaseNotes

class ReleaseNotesGenerator {

    /**
     * Synthesizes draft release notes from real project change summary data.
     */
    fun generateDraftNotes(
        version: String,
        projectName: String,
        changeSummary: ReleaseChangeSummary
    ): ReleaseNotes {
        val features = mutableListOf<String>()
        val fixes = mutableListOf<String>()
        val breakingChanges = mutableListOf<String>()
        val migrationNotes = mutableListOf<String>()

        // Analyze added files for feature notes
        changeSummary.filesAdded.forEach { file ->
            val cleanName = file.substringAfterLast("/")
            when {
                cleanName.contains("Screen") || cleanName.contains("Page") -> features.add("Added $cleanName user interface view.")
                cleanName.contains("Service") || cleanName.contains("Repository") -> features.add("Integrated $cleanName business logic component.")
                cleanName.contains("Entity") || cleanName.contains("Dao") -> features.add("Added $cleanName data layer component.")
                else -> features.add("Created new file $cleanName.")
            }
        }

        // Analyze modified files
        changeSummary.filesModified.forEach { file ->
            val cleanName = file.substringAfterLast("/")
            when {
                cleanName.contains("Fix") || cleanName.contains("Error") -> fixes.add("Resolved issues in $cleanName.")
                cleanName.contains("Database") || cleanName.contains("Schema") -> migrationNotes.add("Updated database structure in $cleanName.")
                else -> features.add("Enhanced functionality in $cleanName.")
            }
        }

        // Incorporate explicit database changes
        changeSummary.databaseChanges.forEach { dbChange ->
            migrationNotes.add("Database Schema: $dbChange")
        }

        // Incorporate AI / User changes
        changeSummary.userChanges.forEach { uChange ->
            features.add("User Update: $uChange")
        }
        changeSummary.aiChanges.forEach { aiChange ->
            features.add("AI Assistance: $aiChange")
        }
        changeSummary.importantFixes.forEach { fix ->
            fixes.add(fix)
        }

        // Fallback defaults if list is minimal
        if (features.isEmpty()) {
            features.add("General quality enhancements and code optimizations for $projectName.")
        }
        if (fixes.isEmpty()) {
            fixes.add("Stability improvements and build validation pass.")
        }

        val summary = "Release $version of $projectName includes ${features.size} feature update(s) and ${fixes.size} fix(es)."

        return ReleaseNotes(
            summary = summary,
            features = features.distinct(),
            fixes = fixes.distinct(),
            breakingChanges = breakingChanges.distinct(),
            migrationNotes = migrationNotes.distinct(),
            isDraft = true
        )
    }
}

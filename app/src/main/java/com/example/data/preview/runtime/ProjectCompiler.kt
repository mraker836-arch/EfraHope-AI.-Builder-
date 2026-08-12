package com.example.data.preview.runtime

import com.example.data.ai.validation.ValidationEngine
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.ErrorType
import com.example.data.preview.PreviewProject

data class CompiledBundle(
    val isSuccess: Boolean,
    val bundleId: String = java.util.UUID.randomUUID().toString(),
    val compiledFilesCount: Int,
    val errors: List<AppError> = emptyList(),
    val warnings: List<AppError> = emptyList(),
    val compilationDurationMs: Long = 0
)

class ProjectCompiler(
    private val validationEngine: ValidationEngine? = null
) {

    fun compile(project: PreviewProject): CompiledBundle {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<AppError>()
        val warnings = mutableListOf<AppError>()

        // 1. Run Validation Engine if provided
        if (validationEngine != null) {
            val entity = ProjectEntity(
                id = project.id,
                name = project.name,
                description = project.description,
                appType = project.appType
            )
            val fileEntities = project.files.map { pf ->
                ProjectFileEntity(
                    fileId = pf.path,
                    projectId = project.id,
                    filePath = pf.path,
                    fileContent = pf.content
                )
            }

            val valResult = validationEngine.validateProject(entity, fileEntities)
            errors.addAll(valResult.errors)
            warnings.addAll(valResult.warnings)
        } else {
            // Internal static check if validation engine not provided
            project.files.forEach { file ->
                if (!file.isBinary) {
                    // Syntax basic validation
                    if (file.content.contains("<<<<<<<") || file.content.contains(">>>>>>>")) {
                        errors.add(
                            AppError(
                                type = ErrorType.SYNTAX,
                                severity = ErrorSeverity.ERROR,
                                message = "Git merge conflict marker detected in file ${file.path}",
                                source = "ProjectCompiler",
                                file = file.path
                            )
                        )
                    }
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val hasCriticalErrors = errors.any { it.severity == ErrorSeverity.ERROR }

        return CompiledBundle(
            isSuccess = !hasCriticalErrors,
            compiledFilesCount = project.files.size,
            errors = errors,
            warnings = warnings,
            compilationDurationMs = duration
        )
    }
}

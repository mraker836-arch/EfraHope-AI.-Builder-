package com.example.data.ai.validation

import com.example.data.ai.agents.TestingAgent
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.ErrorStatus
import com.example.data.models.ErrorType
import com.example.data.models.ProjectHealth
import java.util.UUID

class TypeChecker {
    fun check(files: List<ProjectFileEntity>): List<AppError> {
        val errors = mutableListOf<AppError>()
        val fileMap = files.associateBy { it.filePath }

        for (file in files) {
            val lines = file.fileContent.lines()
            for ((idx, line) in lines.withIndex()) {
                val trim = line.trim()
                if (trim.contains(": any") || trim.contains("as any")) {
                    errors.add(
                        AppError(
                            id = UUID.randomUUID().toString(),
                            type = ErrorType.TYPE,
                            severity = ErrorSeverity.WARNING,
                            message = "Usage of 'any' type on line ${idx + 1}",
                            source = "TypeChecker",
                            file = file.filePath,
                            line = idx + 1,
                            code = trim,
                            status = ErrorStatus.OPEN,
                            suggestedSolution = "Specify explicit TypeScript type or interface."
                        )
                    )
                }
            }
        }
        return errors
    }
}

class Linter {
    fun check(files: List<ProjectFileEntity>): List<AppError> {
        val warnings = mutableListOf<AppError>()
        for (file in files) {
            if (file.fileContent.contains("var ")) {
                warnings.add(
                    AppError(
                        id = UUID.randomUUID().toString(),
                        type = ErrorType.SYNTAX,
                        severity = ErrorSeverity.WARNING,
                        message = "Use of 'var' keyword found",
                        source = "Linter",
                        file = file.filePath,
                        status = ErrorStatus.OPEN,
                        suggestedSolution = "Replace 'var' with 'const' or 'let'."
                    )
                )
            }
        }
        return warnings
    }
}

class BuildValidator {
    fun check(files: List<ProjectFileEntity>): List<AppError> {
        val errors = mutableListOf<AppError>()
        val hasMain = files.any { it.isMain || it.filePath.contains("index") || it.filePath.contains("App") }
        if (!hasMain && files.isNotEmpty()) {
            errors.add(
                AppError(
                    id = UUID.randomUUID().toString(),
                    type = ErrorType.BUILD,
                    severity = ErrorSeverity.ERROR,
                    message = "No main entry point file found (e.g. App.tsx or index.tsx)",
                    source = "BuildValidator",
                    status = ErrorStatus.OPEN,
                    possibleCause = "Missing root App or index component file.",
                    suggestedSolution = "Create src/App.tsx or src/index.tsx as main entry point."
                )
            )
        }
        return errors
    }
}

class TestRunner(private val testingAgent: TestingAgent = TestingAgent()) {
    fun run(project: ProjectEntity, files: List<ProjectFileEntity>): Pair<List<AppError>, List<AppError>> {
        val report = testingAgent.runProjectTests(project, files)
        return Pair(report.errors, report.warnings)
    }
}

data class ValidationEngineResult(
    val isValid: Boolean,
    val health: ProjectHealth,
    val errors: List<AppError>,
    val warnings: List<AppError>,
    val durationMs: Long
)

class ValidationEngine(
    private val testingAgent: TestingAgent = TestingAgent(),
    private val typeChecker: TypeChecker = TypeChecker(),
    private val linter: Linter = Linter(),
    private val buildValidator: BuildValidator = BuildValidator()
) {
    private val testRunner = TestRunner(testingAgent)

    fun validateProject(
        project: ProjectEntity,
        files: List<ProjectFileEntity>
    ): ValidationEngineResult {
        val startTime = System.currentTimeMillis()

        val (agentErrors, agentWarnings) = testRunner.run(project, files)
        val typeErrors = typeChecker.check(files)
        val lintWarnings = linter.check(files)
        val buildErrors = buildValidator.check(files)

        val allErrors = (agentErrors + buildErrors).toMutableList()
        val allWarnings = (agentWarnings + typeErrors + lintWarnings).toMutableList()

        // Error Grouping: Link downstream errors to root error
        val importErrors = allErrors.filter { it.type == ErrorType.IMPORT }
        if (importErrors.isNotEmpty()) {
            val rootImportError = importErrors.first()
            for (i in allErrors.indices) {
                val err = allErrors[i]
                if (err.id != rootImportError.id && err.file == rootImportError.file && err.rootErrorId == null) {
                    allErrors[i] = err.copy(rootErrorId = rootImportError.id)
                }
            }
        }

        val isValid = allErrors.isEmpty()
        val health = when {
            files.isEmpty() -> ProjectHealth.UNKNOWN
            allErrors.isNotEmpty() -> ProjectHealth.CRITICAL
            allWarnings.isNotEmpty() -> ProjectHealth.WARNING
            else -> ProjectHealth.HEALTHY
        }

        val duration = System.currentTimeMillis() - startTime
        return ValidationEngineResult(
            isValid = isValid,
            health = health,
            errors = allErrors,
            warnings = allWarnings,
            durationMs = duration
        )
    }
}

package com.example.data.ai.agents

import com.example.data.ai.models.*
import com.example.data.ai.validation.ValidationEngine
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.*
import java.util.UUID

class ErrorFixingAgent(
    private val validationEngine: ValidationEngine = ValidationEngine()
) {

    fun analyzeAndProposeFix(
        error: AppError,
        project: ProjectEntity,
        files: List<ProjectFileEntity>
    ): FixProposal {
        val opId = UUID.randomUUID().toString()
        val affectedFilePath = error.file ?: files.firstOrNull()?.filePath ?: "src/App.tsx"
        val targetFile = files.find { it.filePath == affectedFilePath }

        val rootCause: String
        val explanation: String
        val changes = mutableListOf<FileChange>()
        val diffs = mutableListOf<FileDiff>()
        var confidence = FixConfidence.HIGH
        var confidenceReason = "Direct root cause identification and minimal targeted fix."
        var riskLevel = "Low"
        var isDestructive = false

        when (error.type) {
            ErrorType.IMPORT -> {
                val badLine = error.code ?: ""
                val importPath = extractPathFromCode(badLine) ?: error.message.substringAfter("'").substringBefore("'")
                
                // Search filesystem for matching file
                val matchedFile = findMatchingFile(importPath, files)
                if (matchedFile != null && targetFile != null) {
                    val correctPath = calculateRelativePath(targetFile.filePath, matchedFile.filePath)
                    val oldContent = targetFile.fileContent
                    val newContent = replaceImportPath(oldContent, importPath, correctPath)

                    rootCause = "Import path '$importPath' was incorrectly referenced. Actual file exists at '${matchedFile.filePath}'."
                    explanation = "Updated relative import path from '$importPath' to '$correctPath'."

                    changes.add(
                        FileChange(
                            operation = ChangeOperation.UPDATE_FILE,
                            targetFilePath = targetFile.filePath,
                            reason = "Correct invalid import path reference",
                            expectedResult = "Resolves module not found error '$importPath'",
                            content = newContent,
                            oldContent = oldContent
                        )
                    )

                    diffs.add(
                        FileDiff(
                            filePath = targetFile.filePath,
                            oldContent = oldContent,
                            newContent = newContent,
                            additions = 1,
                            deletions = 1,
                            operation = ChangeOperation.UPDATE_FILE
                        )
                    )
                } else if (targetFile != null) {
                    // Create missing component file
                    val newFilePath = suggestFilePathForImport(targetFile.filePath, importPath)
                    val componentName = importPath.substringAfterLast("/").capitalizeFirstLetter()
                    val newFileContent = """
                        import React from 'react';

                        export const $componentName: React.FC = () => {
                            return (
                                <div className="p-4 bg-slate-800 text-white rounded-lg">
                                    <h3 className="font-bold">$componentName Component</h3>
                                    <p className="text-sm text-slate-400">Generated auto-fix component placeholder.</p>
                                </div>
                            );
                        };

                        export default $componentName;
                    """.trimIndent()

                    rootCause = "Target imported file '$importPath' does not exist in project filesystem."
                    explanation = "Created missing component file at '$newFilePath'."
                    confidence = FixConfidence.MEDIUM
                    confidenceReason = "Created missing placeholder file to resolve broken import."

                    changes.add(
                        FileChange(
                            operation = ChangeOperation.CREATE_FILE,
                            targetFilePath = newFilePath,
                            reason = "Create missing imported module/component",
                            expectedResult = "Resolves missing module error by providing target file",
                            content = newFileContent
                        )
                    )

                    diffs.add(
                        FileDiff(
                            filePath = newFilePath,
                            oldContent = "",
                            newContent = newFileContent,
                            additions = newFileContent.lines().size,
                            deletions = 0,
                            operation = ChangeOperation.CREATE_FILE
                        )
                    )
                } else {
                    rootCause = "Affected file or target import missing."
                    explanation = "Unable to locate target file for import fix."
                    confidence = FixConfidence.LOW
                    confidenceReason = "File not found in workspace."
                    riskLevel = "High"
                }
            }

            ErrorType.SYNTAX -> {
                if (targetFile != null) {
                    val oldContent = targetFile.fileContent
                    val newContent = fixSyntaxInContent(oldContent, error)

                    rootCause = "Syntax bracket/parenthesis mismatch or unclosed token at line ${error.line ?: "unknown"}."
                    explanation = "Added missing closing bracket/parenthesis to balance syntax."

                    changes.add(
                        FileChange(
                            operation = ChangeOperation.UPDATE_FILE,
                            targetFilePath = targetFile.filePath,
                            reason = "Fix unclosed syntax bracket",
                            expectedResult = "Resolves syntax error",
                            content = newContent,
                            oldContent = oldContent
                        )
                    )

                    diffs.add(
                        FileDiff(
                            filePath = targetFile.filePath,
                            oldContent = oldContent,
                            newContent = newContent,
                            additions = 1,
                            deletions = 0,
                            operation = ChangeOperation.UPDATE_FILE
                        )
                    )
                } else {
                    rootCause = "Syntax error in missing file."
                    explanation = "Target file not found."
                    confidence = FixConfidence.LOW
                    confidenceReason = "Target file missing."
                }
            }

            ErrorType.COMPONENT -> {
                if (targetFile != null) {
                    val oldContent = targetFile.fileContent
                    val compName = targetFile.filePath.substringAfterLast("/").substringBefore(".").capitalizeFirstLetter()
                    val newContent = if (oldContent.isBlank()) {
                        """
                            import React from 'react';

                            export const $compName: React.FC = () => {
                                return (
                                    <div className="p-4 bg-slate-800 text-white rounded-md">
                                        <h2 className="text-lg font-semibold">$compName</h2>
                                    </div>
                                );
                            };

                            export default $compName;
                        """.trimIndent()
                    } else {
                        "$oldContent\n\nexport default $compName;"
                    }

                    rootCause = "Component file is empty or missing export statement."
                    explanation = "Added React component boilerplate and default export."

                    changes.add(
                        FileChange(
                            operation = ChangeOperation.UPDATE_FILE,
                            targetFilePath = targetFile.filePath,
                            reason = "Add component boilerplate and export",
                            expectedResult = "Resolves empty component issue",
                            content = newContent,
                            oldContent = oldContent
                        )
                    )

                    diffs.add(
                        FileDiff(
                            filePath = targetFile.filePath,
                            oldContent = oldContent,
                            newContent = newContent,
                            additions = 5,
                            deletions = 0,
                            operation = ChangeOperation.UPDATE_FILE
                        )
                    )
                } else {
                    rootCause = "Component file not found."
                    explanation = "Target file not available."
                    confidence = FixConfidence.LOW
                    confidenceReason = "Target file missing."
                }
            }

            ErrorType.BUILD -> {
                val newFilePath = "src/App.tsx"
                val mainContent = """
                    import React from 'react';

                    export const App: React.FC = () => {
                        return (
                            <div className="min-h-screen bg-slate-900 text-white p-8">
                                <h1 className="text-2xl font-bold">${project.name}</h1>
                                <p className="text-slate-400 mt-2">${project.description}</p>
                            </div>
                        );
                    };

                    export default App;
                """.trimIndent()

                rootCause = "Missing main entry point file (src/App.tsx or src/index.tsx)."
                explanation = "Created main App entry point file at 'src/App.tsx'."

                changes.add(
                    FileChange(
                        operation = ChangeOperation.CREATE_FILE,
                        targetFilePath = newFilePath,
                        reason = "Create root entry point file",
                        expectedResult = "Provides main App component for build system",
                        content = mainContent
                    )
                )

                diffs.add(
                    FileDiff(
                        filePath = newFilePath,
                        oldContent = "",
                        newContent = mainContent,
                        additions = mainContent.lines().size,
                        deletions = 0,
                        operation = ChangeOperation.CREATE_FILE
                    )
                )
            }

            else -> {
                if (targetFile != null) {
                    val oldContent = targetFile.fileContent
                    val newContent = "$oldContent\n// AI Auto-fix applied for: ${error.message}"
                    rootCause = "Generic error in file '${targetFile.filePath}': ${error.message}"
                    explanation = "Applied targeted annotation fix for ${error.type}."
                    confidence = FixConfidence.LOW
                    confidenceReason = "Generic error type; applied basic non-destructive fix."

                    changes.add(
                        FileChange(
                            operation = ChangeOperation.UPDATE_FILE,
                            targetFilePath = targetFile.filePath,
                            reason = "Apply targeted fix annotation",
                            expectedResult = "Annotates error location",
                            content = newContent,
                            oldContent = oldContent
                        )
                    )

                    diffs.add(
                        FileDiff(
                            filePath = targetFile.filePath,
                            oldContent = oldContent,
                            newContent = newContent,
                            additions = 1,
                            deletions = 0,
                            operation = ChangeOperation.UPDATE_FILE
                        )
                    )
                } else {
                    rootCause = "Unknown error source."
                    explanation = "No file targeted for fix."
                    confidence = FixConfidence.LOW
                    confidenceReason = "Unresolved error source."
                }
            }
        }

        val changePlan = ChangePlan(
            operationId = opId,
            intent = "FIX_ERROR",
            summary = "AI Auto-Fix for Error: ${error.message}",
            affectedFiles = changes.map { it.targetFilePath }.distinct(),
            changes = changes,
            diffs = diffs,
            risks = if (riskLevel == "High") listOf("High risk of side effects", "Multiple files modified") else listOf("Low risk targeted change"),
            riskLevel = riskLevel,
            validationRequired = true,
            explanation = explanation,
            approved = false,
            status = "Pending"
        )

        // Pre-validate Proposed Fix in Simulated File Set
        val simulatedFiles = applyChangesToSimulatedFiles(files, changes)
        val validationEngineResult = validationEngine.validateProject(project, simulatedFiles)
        val valResult = ValidationResult(
            isValid = validationEngineResult.isValid,
            errors = validationEngineResult.errors.map { it.message },
            warnings = validationEngineResult.warnings.map { it.message }
        )

        return FixProposal(
            errorId = error.id,
            rootCause = rootCause,
            explanation = explanation,
            changePlan = changePlan,
            confidence = confidence,
            confidenceReason = confidenceReason,
            affectedFiles = changePlan.affectedFiles,
            expectedResult = changes.firstOrNull()?.expectedResult ?: "Resolves error",
            risk = riskLevel,
            isDestructive = isDestructive,
            validationResult = valResult
        )
    }

    private fun extractPathFromCode(line: String): String? {
        val fromIdx = line.indexOf("from ")
        if (fromIdx != -1) return line.substring(fromIdx + 5).trim(' ', ';', '"', '\'')
        val reqIdx = line.indexOf("require(")
        if (reqIdx != -1) {
            val endIdx = line.indexOf(")", reqIdx)
            if (endIdx != -1) return line.substring(reqIdx + 8, endIdx).trim(' ', '"', '\'')
        }
        return null
    }

    private fun findMatchingFile(importPath: String, files: List<ProjectFileEntity>): ProjectFileEntity? {
        val fileName = importPath.substringAfterLast("/")
        return files.find { f ->
            val fName = f.filePath.substringAfterLast("/").substringBefore(".")
            fName.equals(fileName, ignoreCase = true)
        }
    }

    private fun calculateRelativePath(fromPath: String, toPath: String): String {
        val fromParts = fromPath.split("/").dropLast(1)
        val toParts = toPath.split("/")
        val toFileName = toParts.last().substringBefore(".")

        var commonIdx = 0
        while (commonIdx < fromParts.size && commonIdx < toParts.size - 1 && fromParts[commonIdx] == toParts[commonIdx]) {
            commonIdx++
        }

        val upCount = fromParts.size - commonIdx
        val upPrefix = if (upCount == 0) "./" else "../".repeat(upCount)
        val subDirs = toParts.subList(commonIdx, toParts.size - 1)

        val relPath = if (subDirs.isEmpty()) {
            "$upPrefix$toFileName"
        } else {
            "$upPrefix${subDirs.joinToString("/")}/$toFileName"
        }
        return relPath
    }

    private fun replaceImportPath(content: String, oldPath: String, newPath: String): String {
        return content.replace("'$oldPath'", "'$newPath'").replace("\"$oldPath\"", "\"$newPath\"")
    }

    private fun suggestFilePathForImport(currentFilePath: String, importPath: String): String {
        val currentDir = currentFilePath.substringBeforeLast("/", "src")
        return if (importPath.startsWith("./")) {
            "$currentDir/${importPath.removePrefix("./")}.tsx"
        } else if (importPath.startsWith("../")) {
            val parentDir = currentDir.substringBeforeLast("/", "src")
            "$parentDir/${importPath.removePrefix("../")}.tsx"
        } else {
            "src/components/${importPath.substringAfterLast("/")}.tsx"
        }
    }

    private fun fixSyntaxInContent(content: String, error: AppError): String {
        var curly = 0
        var paren = 0
        var square = 0
        for (c in content) {
            when (c) {
                '{' -> curly++
                '}' -> curly--
                '(' -> paren++
                ')' -> paren--
                '[' -> square++
                ']' -> square--
            }
        }
        var fix = content
        if (curly > 0) fix += "\n" + "}".repeat(curly)
        if (paren > 0) fix += ")".repeat(paren)
        if (square > 0) fix += "]".repeat(square)
        return fix
    }

    private fun applyChangesToSimulatedFiles(
        originalFiles: List<ProjectFileEntity>,
        changes: List<FileChange>
    ): List<ProjectFileEntity> {
        val result = originalFiles.toMutableList()
        for (ch in changes) {
            when (ch.operation) {
                ChangeOperation.CREATE_FILE -> {
                    result.add(
                        ProjectFileEntity(
                            fileId = UUID.randomUUID().toString(),
                            projectId = originalFiles.firstOrNull()?.projectId ?: "",
                            filePath = ch.targetFilePath,
                            fileContent = ch.content ?: ""
                        )
                    )
                }
                ChangeOperation.UPDATE_FILE -> {
                    val idx = result.indexOfFirst { it.filePath == ch.targetFilePath }
                    if (idx != -1) {
                        result[idx] = result[idx].copy(fileContent = ch.content ?: result[idx].fileContent)
                    }
                }
                ChangeOperation.DELETE_FILE -> {
                    result.removeAll { it.filePath == ch.targetFilePath }
                }
                ChangeOperation.RENAME_FILE -> {
                    val idx = result.indexOfFirst { it.filePath == ch.targetFilePath }
                    if (idx != -1 && ch.newFilePath != null) {
                        result[idx] = result[idx].copy(filePath = ch.newFilePath)
                    }
                }
            }
        }
        return result
    }

    private fun String.capitalizeFirstLetter(): String {
        return if (isNotEmpty()) this[0].uppercaseChar() + substring(1) else this
    }
}

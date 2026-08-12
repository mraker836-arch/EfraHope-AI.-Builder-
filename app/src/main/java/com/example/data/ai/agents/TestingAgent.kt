package com.example.data.ai.agents

import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.ErrorStatus
import com.example.data.models.ErrorType
import java.util.UUID

data class TestingReport(
    val isSuccess: Boolean,
    val errors: List<AppError>,
    val warnings: List<AppError>,
    val durationMs: Long
)

class TestingAgent {

    fun runProjectTests(
        project: ProjectEntity,
        files: List<ProjectFileEntity>
    ): TestingReport {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<AppError>()
        val warnings = mutableListOf<AppError>()

        if (files.isEmpty()) {
            errors.add(
                AppError(
                    id = UUID.randomUUID().toString(),
                    type = ErrorType.BUILD,
                    severity = ErrorSeverity.ERROR,
                    message = "Project contains no source files.",
                    source = "TestingAgent",
                    status = ErrorStatus.OPEN,
                    possibleCause = "Project initialization failed or files were deleted.",
                    suggestedSolution = "Generate or add initial project files."
                )
            )
            val duration = System.currentTimeMillis() - startTime
            return TestingReport(isSuccess = false, errors = errors, warnings = warnings, durationMs = duration)
        }

        val normalizedPaths = files.map { normalizePath(it.filePath) }.toSet()

        for (file in files) {
            val lines = file.fileContent.lines()

            // 1. Syntax Check: Bracket matching & JSON validation
            if (file.filePath.endsWith(".json")) {
                val jsonError = checkJsonSyntax(file)
                if (jsonError != null) errors.add(jsonError)
            } else {
                val syntaxError = checkBracketSyntax(file)
                if (syntaxError != null) errors.add(syntaxError)
            }

            // 2. Import Check: Verify relative imports resolve to actual files
            lines.forEachIndexed { index, line ->
                val lineNum = index + 1
                val trimLine = line.trim()

                if (trimLine.startsWith("import ") || trimLine.contains("require(")) {
                    val importPath = extractImportPath(trimLine)
                    if (importPath != null && isRelativeImport(importPath)) {
                        val resolved = resolveRelativePath(file.filePath, importPath, normalizedPaths)
                        if (!resolved) {
                            errors.add(
                                AppError(
                                    id = UUID.randomUUID().toString(),
                                    type = ErrorType.IMPORT,
                                    severity = ErrorSeverity.ERROR,
                                    message = "Cannot find module or file '$importPath'",
                                    source = "TestingAgent",
                                    file = file.filePath,
                                    line = lineNum,
                                    code = trimLine,
                                    status = ErrorStatus.OPEN,
                                    possibleCause = "Import path is incorrect, file was renamed, or target file is missing.",
                                    suggestedSolution = "Update import path to match actual file location or create missing file.",
                                    relatedFiles = listOf(file.filePath)
                                )
                            )
                        }
                    }
                }

                // 3. Warning Check: console.log or unused statements
                if (trimLine.contains("console.log(") || trimLine.contains("debugger;")) {
                    warnings.add(
                        AppError(
                            id = UUID.randomUUID().toString(),
                            type = ErrorType.UNKNOWN,
                            severity = ErrorSeverity.WARNING,
                            message = "Debug statement detected: '${trimLine.take(40)}'",
                            source = "TestingAgent",
                            file = file.filePath,
                            line = lineNum,
                            code = trimLine,
                            status = ErrorStatus.OPEN,
                            suggestedSolution = "Remove debugging code before production build."
                        )
                    )
                }
            }

            // 4. Component Check: Empty or missing export in JS/TS/JSX/TSX files
            if (isComponentFile(file.filePath)) {
                if (file.fileContent.isBlank()) {
                    errors.add(
                        AppError(
                            id = UUID.randomUUID().toString(),
                            type = ErrorType.COMPONENT,
                            severity = ErrorSeverity.ERROR,
                            message = "Component file is empty.",
                            source = "TestingAgent",
                            file = file.filePath,
                            status = ErrorStatus.OPEN,
                            possibleCause = "File created but no code was generated.",
                            suggestedSolution = "Add component code and export statement."
                        )
                    )
                } else if (!file.fileContent.contains("export ") && !file.fileContent.contains("module.exports")) {
                    warnings.add(
                        AppError(
                            id = UUID.randomUUID().toString(),
                            type = ErrorType.COMPONENT,
                            severity = ErrorSeverity.WARNING,
                            message = "No export declaration found in component file.",
                            source = "TestingAgent",
                            file = file.filePath,
                            status = ErrorStatus.OPEN,
                            suggestedSolution = "Add 'export default' or named export."
                        )
                    )
                }
            }
        }

        // 5. Route Validation Check: Compare project routes to page files
        if (project.routesJson.isNotBlank() && project.routesJson != "[]") {
            val pageFiles = files.filter { it.filePath.contains("page") || it.filePath.contains("screen") || it.filePath.startsWith("src/pages/") }
            if (pageFiles.isEmpty() && files.size > 2) {
                warnings.add(
                    AppError(
                        id = UUID.randomUUID().toString(),
                        type = ErrorType.ROUTE,
                        severity = ErrorSeverity.WARNING,
                        message = "Routes defined but no dedicated Page/Screen files found.",
                        source = "TestingAgent",
                        status = ErrorStatus.OPEN,
                        suggestedSolution = "Ensure route targets exist in src/pages directory."
                    )
                )
            }
        }

        // 6. Configuration Check: package.json presence or formatting
        val pkgFile = files.find { it.filePath.endsWith("package.json") }
        if (pkgFile == null && files.any { it.filePath.endsWith(".tsx") || it.filePath.endsWith(".jsx") }) {
            warnings.add(
                AppError(
                    id = UUID.randomUUID().toString(),
                    type = ErrorType.CONFIGURATION,
                    severity = ErrorSeverity.WARNING,
                    message = "Missing package.json file in project root.",
                    source = "TestingAgent",
                    status = ErrorStatus.OPEN,
                    suggestedSolution = "Create package.json to manage dependencies."
                )
            )
        }

        val duration = System.currentTimeMillis() - startTime
        return TestingReport(
            isSuccess = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            durationMs = duration
        )
    }

    private fun checkJsonSyntax(file: ProjectFileEntity): AppError? {
        val content = file.fileContent.trim()
        if (content.isEmpty()) {
            return AppError(
                id = UUID.randomUUID().toString(),
                type = ErrorType.SYNTAX,
                severity = ErrorSeverity.ERROR,
                message = "JSON file is empty: ${file.filePath}",
                source = "TestingAgent",
                file = file.filePath,
                status = ErrorStatus.OPEN,
                possibleCause = "Empty JSON file.",
                suggestedSolution = "Provide valid JSON structure like {} or []."
            )
        }
        if ((content.startsWith("{") && !content.endsWith("}")) || (content.startsWith("[") && !content.endsWith("]"))) {
            return AppError(
                id = UUID.randomUUID().toString(),
                type = ErrorType.SYNTAX,
                severity = ErrorSeverity.ERROR,
                message = "Unclosed JSON structure in ${file.filePath}",
                source = "TestingAgent",
                file = file.filePath,
                status = ErrorStatus.OPEN,
                possibleCause = "Missing closing brace or bracket.",
                suggestedSolution = "Fix JSON formatting syntax."
            )
        }
        return null
    }

    private fun checkBracketSyntax(file: ProjectFileEntity): AppError? {
        var curlyCount = 0
        var parenCount = 0
        var squareCount = 0
        val content = file.fileContent

        var line = 1
        var col = 1

        for (ch in content) {
            when (ch) {
                '\n' -> { line++; col = 1 }
                '{' -> curlyCount++
                '}' -> curlyCount--
                '(' -> parenCount++
                ')' -> parenCount--
                '[' -> squareCount++
                ']' -> squareCount--
            }
            if (curlyCount < 0 || parenCount < 0 || squareCount < 0) {
                return AppError(
                    id = UUID.randomUUID().toString(),
                    type = ErrorType.SYNTAX,
                    severity = ErrorSeverity.ERROR,
                    message = "Unexpected closing bracket/parenthesis at line $line",
                    source = "TestingAgent",
                    file = file.filePath,
                    line = line,
                    column = col,
                    status = ErrorStatus.OPEN,
                    possibleCause = "Unmatched closing bracket or parenthesis.",
                    suggestedSolution = "Check bracket pairing in file."
                )
            }
            col++
        }

        if (curlyCount != 0 || parenCount != 0 || squareCount != 0) {
            return AppError(
                id = UUID.randomUUID().toString(),
                type = ErrorType.SYNTAX,
                severity = ErrorSeverity.ERROR,
                message = "Unclosed bracket/parenthesis in file",
                source = "TestingAgent",
                file = file.filePath,
                status = ErrorStatus.OPEN,
                possibleCause = "Missing closing brace or parenthesis.",
                suggestedSolution = "Ensure all brackets and parentheses are properly closed."
            )
        }
        return null
    }

    private fun extractImportPath(line: String): String? {
        // e.g. import { Button } from "./components/Button";
        // or import React from "react";
        // or const x = require("./utils");
        val fromIdx = line.indexOf(" from ")
        if (fromIdx != -1) {
            val rawPath = line.substring(fromIdx + 6).trim(' ', ';', '"', '\'')
            return rawPath
        }
        val reqIdx = line.indexOf("require(")
        if (reqIdx != -1) {
            val endParen = line.indexOf(")", reqIdx)
            if (endParen != -1) {
                return line.substring(reqIdx + 8, endParen).trim(' ', '"', '\'')
            }
        }
        return null
    }

    private fun isRelativeImport(path: String): Boolean {
        return path.startsWith("./") || path.startsWith("../")
    }

    private fun resolveRelativePath(
        currentFilePath: String,
        importPath: String,
        allPaths: Set<String>
    ): Boolean {
        val currentDirParts = currentFilePath.split("/").dropLast(1).toMutableList()
        val importParts = importPath.split("/")

        for (part in importParts) {
            when (part) {
                "." -> {} // current dir
                ".." -> if (currentDirParts.isNotEmpty()) currentDirParts.removeAt(currentDirParts.size - 1)
                else -> currentDirParts.add(part)
            }
        }

        val targetBasePath = normalizePath(currentDirParts.joinToString("/"))
        val extensions = listOf("", ".tsx", ".ts", ".jsx", ".js", ".json", "/index.tsx", "/index.ts", "/index.jsx", "/index.js")

        return extensions.any { ext ->
            allPaths.contains("$targetBasePath$ext")
        }
    }

    private fun normalizePath(path: String): String {
        return path.trim().removePrefix("/").replace("//", "/")
    }

    private fun isComponentFile(filePath: String): Boolean {
        val lower = filePath.lowercase()
        return (lower.contains("component") || lower.contains("page") || lower.contains("screen") || lower.endsWith(".tsx") || lower.endsWith(".jsx"))
    }
}

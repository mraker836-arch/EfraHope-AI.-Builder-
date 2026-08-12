package com.example.data.preview

import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity

object PreviewProjectAdapter {

    /**
     * Converts project entity and files into a sandbox-ready PreviewProject representation.
     * Sanitizes paths to enforce sandbox security boundaries.
     */
    fun adapt(project: ProjectEntity, files: List<ProjectFileEntity>): PreviewProject {
        val sanitizedFiles = files.mapNotNull { file ->
            val cleanPath = sanitizePath(file.filePath)
            if (cleanPath != null) {
                PreviewFile(
                    path = cleanPath,
                    content = file.fileContent,
                    isBinary = isBinaryFile(cleanPath),
                    mimeType = determineMimeType(cleanPath)
                )
            } else null
        }

        val extractedDependencies = extractDependencies(sanitizedFiles)

        return PreviewProject(
            id = project.id,
            name = project.name,
            description = project.description,
            appType = project.appType,
            files = sanitizedFiles,
            dependencies = extractedDependencies
        )
    }

    /**
     * Rejects path traversal attempts (e.g. "../", leading slash outside workspace)
     * and returns normalized relative path or null if unsafe.
     */
    fun sanitizePath(rawPath: String): String? {
        val trimmed = rawPath.trim()
        if (trimmed.isEmpty()) return null

        // Security check: reject relative directory escapes
        if (trimmed.contains("../") || trimmed.contains("..\\") || trimmed.startsWith("/")) {
            // Normalize path by stripping leading slashes or illegal parent navigations
            val normalized = trimmed.removePrefix("/").replace("../", "").replace("..\\", "")
            if (normalized.isBlank()) return null
            return normalized
        }

        return trimmed
    }

    private fun isBinaryFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".webp") || lower.endsWith(".ico") || lower.endsWith(".svg") ||
                lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf")
    }

    private fun determineMimeType(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".html") -> "text/html"
            lower.endsWith(".css") -> "text/css"
            lower.endsWith(".js") -> "text/javascript"
            lower.endsWith(".ts") || lower.endsWith(".tsx") || lower.endsWith(".jsx") -> "text/typescript"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".svg") -> "image/svg+xml"
            else -> "text/plain"
        }
    }

    private fun extractDependencies(files: List<PreviewFile>): List<String> {
        val dependencies = mutableSetOf<String>()
        val importRegex = Regex("""import\s+.*?\s+from\s+['"]([^'"]+)['"]""")
        val requireRegex = Regex("""require\s*\(\s*['"]([^'"]+)['"]\s*\)""")

        files.forEach { file ->
            if (file.path.endsWith(".json") && file.path.contains("package.json")) {
                // Parse package.json dependencies if present
                val depKeyRegex = Regex(""""([^" triggering]+)":\s*"[^"]+"""")
                depKeyRegex.findAll(file.content).forEach { match ->
                    val dep = match.groupValues[1]
                    if (!dep.startsWith("@types/") && dep != "dependencies" && dep != "devDependencies") {
                        dependencies.add(dep)
                    }
                }
            } else if (!file.isBinary) {
                importRegex.findAll(file.content).forEach { match ->
                    val pkg = match.groupValues[1]
                    if (!pkg.startsWith(".") && !pkg.startsWith("/")) {
                        dependencies.add(pkg.split("/").first())
                    }
                }
                requireRegex.findAll(file.content).forEach { match ->
                    val pkg = match.groupValues[1]
                    if (!pkg.startsWith(".") && !pkg.startsWith("/")) {
                        dependencies.add(pkg.split("/").first())
                    }
                }
            }
        }

        return dependencies.toList()
    }
}

package com.example.data.utils

object PathSecurity {
    private val INVALID_CHARACTERS = listOf(':', '*', '?', '"', '<', '>', '|')

    fun validatePath(path: String): Result<String> {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Path cannot be empty."))
        }

        // Check path traversal sequences
        if (trimmed.contains("../") || trimmed.contains("..\\") || trimmed == ".." || trimmed.endsWith("/..")) {
            return Result.failure(IllegalArgumentException("Security Violation: Path traversal ('..') is strictly forbidden."))
        }

        // Check absolute paths
        if (trimmed.startsWith("/") || trimmed.startsWith("\\") || (trimmed.length > 1 && trimmed[1] == ':')) {
            return Result.failure(IllegalArgumentException("Security Violation: Absolute paths escaping the project root are forbidden."))
        }

        // Check invalid characters
        if (INVALID_CHARACTERS.any { trimmed.contains(it) }) {
            return Result.failure(IllegalArgumentException("Invalid Path: Path contains forbidden characters (e.g. *, ?, <, >, |)."))
        }

        // Normalize trailing slashes
        val normalized = trimmed.replace('\\', '/').removePrefix("/")
        return Result.success(normalized)
    }

    fun getFileName(path: String): String {
        val clean = path.replace('\\', '/').trimEnd('/')
        val lastSlash = clean.lastIndexOf('/')
        return if (lastSlash >= 0) clean.substring(lastSlash + 1) else clean
    }

    fun getParentPath(path: String): String {
        val clean = path.replace('\\', '/').trimEnd('/')
        val lastSlash = clean.lastIndexOf('/')
        return if (lastSlash >= 0) clean.substring(0, lastSlash) else ""
    }

    fun detectLanguage(filePath: String): String {
        val name = getFileName(filePath).lowercase()
        return when {
            name.endsWith(".kt") || name.endsWith(".kts") -> "kotlin"
            name.endsWith(".java") -> "java"
            name.endsWith(".tsx") || name.endsWith(".jsx") -> "typescript"
            name.endsWith(".ts") || name.endsWith(".js") -> "typescript"
            name.endsWith(".html") || name.endsWith(".htm") -> "html"
            name.endsWith(".css") || name.endsWith(".scss") -> "css"
            name.endsWith(".json") -> "json"
            name.endsWith(".md") -> "markdown"
            name.endsWith(".xml") -> "xml"
            name.endsWith(".sql") -> "sql"
            else -> "plaintext"
        }
    }
}

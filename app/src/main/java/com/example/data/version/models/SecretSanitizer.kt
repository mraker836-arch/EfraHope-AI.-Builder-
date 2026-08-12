package com.example.data.version.models

object SecretSanitizer {

    private val SECRET_PATTERNS = listOf(
        Regex("(?i)(api[_-]?key|secret|password|bearer|auth[_-]?token|private[_-]?key|access[_-]?token)\\s*[:=]\\s*[\"']?([^\"'\\s]+)[\"']?"),
        Regex("(?i)AIzaSy[A-Za-z0-9_-]{33}"), // Google API keys
        Regex("(?i)sk-[A-Za-z0-9]{32,}")       // OpenAI keys
    )

    fun sanitizeContent(content: String): String {
        var sanitized = content
        for (pattern in SECRET_PATTERNS) {
            sanitized = pattern.replace(sanitized) { matchResult ->
                val fullMatch = matchResult.value
                if (matchResult.groupValues.size >= 3) {
                    val keyName = matchResult.groupValues[1]
                    "$keyName=\"[REDACTED_SECRET]\""
                } else {
                    "[REDACTED_SECRET]"
                }
            }
        }
        return sanitized
    }

    fun sanitizeFiles(files: List<FileSnapshot>): List<FileSnapshot> {
        return files.map { file ->
            file.copy(content = sanitizeContent(file.content))
        }
    }
}

package com.example.data.preview.runtime

import com.example.data.preview.PreviewFile
import com.example.data.preview.PreviewProject

data class TransformedProject(
    val project: PreviewProject,
    val executableFiles: List<PreviewFile>,
    val sanitizedCount: Int
)

class ProjectTransformer {

    fun transform(project: PreviewProject): TransformedProject {
        var sanitizedCount = 0

        val transformedFiles = project.files.map { file ->
            if (file.isBinary) {
                file
            } else {
                val (cleanContent, modified) = sanitizeCode(file.content)
                if (modified) sanitizedCount++
                file.copy(content = cleanContent)
            }
        }

        return TransformedProject(
            project = project.copy(files = transformedFiles),
            executableFiles = transformedFiles,
            sanitizedCount = sanitizedCount
        )
    }

    private fun sanitizeCode(code: String): Pair<String, Boolean> {
        var modified = false
        var result = code

        // Security check 1: Strip host process execution calls
        if (result.contains("process.env.") && result.contains("SECRET")) {
            result = result.replace(Regex("""process\.env\.[A-Z0-9_]*SECRET[A-Z0-9_]*"""), "\"***REDACTED_SECRET***\"")
            modified = true
        }

        // Security check 2: Prevent unsafe global access patterns
        if (result.contains("window.top.location") || result.contains("window.parent.location")) {
            result = result.replace("window.top.location", "/* blocked */ null")
            result = result.replace("window.parent.location", "/* blocked */ null")
            modified = true
        }

        return Pair(result, modified)
    }
}

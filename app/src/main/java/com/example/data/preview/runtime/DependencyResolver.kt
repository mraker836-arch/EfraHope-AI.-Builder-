package com.example.data.preview.runtime

import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.ErrorType
import com.example.data.preview.PreviewProject

data class DependencyResolutionResult(
    val isValid: Boolean,
    val resolvedDependencies: List<String>,
    val missingDependencies: List<String>,
    val errors: List<AppError> = emptyList(),
    val warnings: List<AppError> = emptyList()
)

class DependencyResolver {

    // Standard known browser/runtime packages pre-approved in sandbox
    private val standardAllowedPackages = setOf(
        "react", "react-dom", "lucide-react", "recharts", "framer-motion",
        "tailwindcss", "axios", "clsx", "tailwind-merge", "zustand", "date-fns"
    )

    fun resolveDependencies(project: PreviewProject): DependencyResolutionResult {
        val detectedDeps = project.dependencies
        val missing = mutableListOf<String>()
        val warnings = mutableListOf<AppError>()
        val errors = mutableListOf<AppError>()

        detectedDeps.forEach { dep ->
            if (!standardAllowedPackages.contains(dep)) {
                // If it's an unrecognized package, raise a warning for controlled review
                val warning = AppError(
                    type = ErrorType.DEPENDENCY,
                    severity = ErrorSeverity.WARNING,
                    message = "Package '$dep' is referenced but may need controlled dependency verification.",
                    source = "DependencyResolver",
                    file = "package.json",
                    possibleCause = "Module '$dep' is missing from sandbox cache.",
                    suggestedSolution = "Ensure required module is included in standard runtime libraries or project configuration."
                )
                warnings.add(warning)
                missing.add(dep)
            }
        }

        return DependencyResolutionResult(
            isValid = errors.isEmpty(),
            resolvedDependencies = detectedDeps.filter { standardAllowedPackages.contains(it) },
            missingDependencies = missing,
            errors = errors,
            warnings = warnings
        )
    }
}

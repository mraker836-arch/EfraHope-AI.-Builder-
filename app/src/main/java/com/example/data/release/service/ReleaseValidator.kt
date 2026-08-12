package com.example.data.release.service

import com.example.data.db.ProjectFileEntity
import com.example.data.db.schema.DatabaseSchema
import com.example.data.release.models.*

class ReleaseValidator {

    /**
     * Validates project state and generates quality gates, blockers, and readiness evaluation.
     */
    fun validateProjectForRelease(
        version: String,
        environment: ReleaseEnvironment,
        files: List<ProjectFileEntity>,
        databaseSchema: DatabaseSchema?,
        hasActiveBuild: Boolean = false,
        lastBuildPassed: Boolean = true,
        lastTestsPassed: Boolean = true,
        criticalErrorCount: Int = 0
    ): ReleaseReadiness {
        val gates = mutableListOf<QualityGateResult>()
        val blockers = mutableListOf<ReleaseBlocker>()

        // 1. Version Required Gate
        if (version.isBlank() || !isValidSemVer(version)) {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.VERSION_REQUIRED,
                    status = GateStatus.FAIL,
                    message = "Release version must follow semantic versioning (MAJOR.MINOR.PATCH e.g. 1.0.0)"
                )
            )
            blockers.add(
                ReleaseBlocker(
                    type = "INVALID_VERSION",
                    title = "Invalid Version Format",
                    description = "Provided version '$version' does not adhere to Semantic Versioning format.",
                    impact = "Prevents deployment tracking and rollback safety.",
                    resolutionSteps = "Specify a valid version string matching X.Y.Z (e.g. 1.0.0)."
                )
            )
        } else {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.VERSION_REQUIRED,
                    status = GateStatus.PASS,
                    message = "Version '$version' is valid semantic versioning."
                )
            )
        }

        // 2. Build Must Pass
        if (!lastBuildPassed) {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.BUILD_MUST_PASS,
                    status = GateStatus.FAIL,
                    message = "Latest project build failed or had compilation errors."
                )
            )
            blockers.add(
                ReleaseBlocker(
                    type = "BUILD_FAILURE",
                    title = "Build System Failure",
                    description = "The project compilation failed during the last build attempt.",
                    impact = "Corrupted or non-executable release artifact.",
                    resolutionSteps = "Fix compilation errors in the Code Editor and re-run build."
                )
            )
        } else {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.BUILD_MUST_PASS,
                    status = GateStatus.PASS,
                    message = "Project code compiles and builds successfully."
                )
            )
        }

        // 3. Tests Must Pass
        if (!lastTestsPassed) {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.TESTS_MUST_PASS,
                    status = GateStatus.FAIL,
                    message = "Unit or integration test suite reported failures."
                )
            )
            blockers.add(
                ReleaseBlocker(
                    type = "TEST_FAILURE",
                    title = "Test Suite Failures",
                    description = "One or more automated tests failed.",
                    impact = "Potential regression bug in production.",
                    resolutionSteps = "Open Testing Dashboard, resolve failing tests, and re-test."
                )
            )
        } else {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.TESTS_MUST_PASS,
                    status = GateStatus.PASS,
                    message = "All test cases passed cleanly."
                )
            )
        }

        // 4. No Critical Errors
        if (criticalErrorCount > 0) {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.NO_CRITICAL_ERRORS,
                    status = GateStatus.FAIL,
                    message = "Detected $criticalErrorCount unresolved critical syntax/runtime error(s)."
                )
            )
            blockers.add(
                ReleaseBlocker(
                    type = "CRITICAL_ERROR",
                    title = "Unresolved Critical Errors",
                    description = "There are $criticalErrorCount critical error items in Error Center.",
                    impact = "High likelihood of crash on launch.",
                    resolutionSteps = "Inspect Error Center and fix critical error diagnostics."
                )
            )
        } else {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.NO_CRITICAL_ERRORS,
                    status = GateStatus.PASS,
                    message = "No critical errors reported in project codebase."
                )
            )
        }

        // 5. Environment & Project Metadata Validation
        val hasManifest = files.any { it.filePath.contains("AndroidManifest.xml") || it.filePath.contains("metadata.json") }
        if (files.isEmpty()) {
            blockers.add(
                ReleaseBlocker(
                    type = "MISSING_FILES",
                    title = "Empty Workspace",
                    description = "Project workspace contains no source files.",
                    impact = "Cannot package empty release candidate.",
                    resolutionSteps = "Generate or upload project files before releasing."
                )
            )
        }

        // 6. Database / Schema Status
        if (databaseSchema != null && environment == ReleaseEnvironment.PRODUCTION) {
            val pendingEntities = databaseSchema.entities
            if (pendingEntities.isEmpty()) {
                gates.add(
                    QualityGateResult(
                        gate = QualityGateType.NO_UNRESOLVED_RELEASE_BLOCKERS,
                        status = GateStatus.WARNING,
                        message = "Database schema has no entities defined."
                    )
                )
            } else {
                gates.add(
                    QualityGateResult(
                        gate = QualityGateType.NO_UNRESOLVED_RELEASE_BLOCKERS,
                        status = GateStatus.PASS,
                        message = "Database schema is valid with ${pendingEntities.size} entities."
                    )
                )
            }
        } else {
            gates.add(
                QualityGateResult(
                    gate = QualityGateType.NO_UNRESOLVED_RELEASE_BLOCKERS,
                    status = GateStatus.PASS,
                    message = "Release readiness check completed."
                )
            )
        }

        // 7. Artifact Required Gate (Pre-requisite for final DEPLOY)
        gates.add(
            QualityGateResult(
                gate = QualityGateType.ARTIFACT_REQUIRED,
                status = if (lastBuildPassed) GateStatus.PASS else GateStatus.FAIL,
                message = if (lastBuildPassed) "Release artifact ready for packaging." else "Build artifact generation pending valid build."
            )
        )

        // Calculate score & status
        val totalGates = gates.size
        val passedGates = gates.count { it.status == GateStatus.PASS }
        val failedGates = gates.count { it.status == GateStatus.FAIL }

        val scorePercent = ((passedGates.toDouble() / totalGates) * 100).toInt()
        val isReady = failedGates == 0 && blockers.isEmpty()

        val readinessLevel = when {
            isReady && gates.any { it.status == GateStatus.WARNING } -> "READY_WITH_WARNINGS"
            isReady -> "READY"
            else -> "NOT_READY"
        }

        val summary = if (isReady) {
            "Project is validated and ready for ${environment.name} release."
        } else {
            "Release blocked by $failedGates failing quality gate(s) and ${blockers.size} blocker(s)."
        }

        return ReleaseReadiness(
            scorePercent = scorePercent,
            isReady = isReady,
            readinessLevel = readinessLevel,
            gates = gates,
            blockers = blockers,
            summary = summary
        )
    }

    private fun isValidSemVer(version: String): Boolean {
        val regex = Regex("""^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$""")
        return regex.matches(version.trim())
    }
}

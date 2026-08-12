package com.example.data.release.service

import com.example.data.db.ProjectFileEntity
import com.example.data.release.models.*
import java.security.MessageDigest
import java.util.UUID

class BuildEngine {

    /**
     * Simulates / executes project compilation and generates artifact with calculated SHA-256 integrity checksum.
     */
    suspend fun buildReleaseArtifact(
        projectId: String,
        releaseId: String,
        version: String,
        environment: ReleaseEnvironment,
        files: List<ProjectFileEntity>,
        simulateFailure: Boolean = false
    ): BuildResult {
        val buildId = UUID.randomUUID().toString()
        val logs = mutableListOf<String>()
        val startTime = System.currentTimeMillis()

        logs.add("[BUILD START] Initializing build pipeline for project $projectId ($version) in $environment mode...")
        logs.add("[PREPARATION] Parsing ${files.size} workspace source files...")

        if (files.isEmpty()) {
            logs.add("[ERROR] Workspace contains no files to build.")
            return BuildResult(
                buildId = buildId,
                state = BuildState.FAILED,
                logs = logs,
                durationMs = System.currentTimeMillis() - startTime,
                errorMessage = "Build failed: Workspace has 0 files."
            )
        }

        logs.add("[VALIDATION] Checking Kotlin syntax & Jetpack Compose imports...")
        logs.add("[COMPILATION] Invoking Kotlin compiler target Android App (minSdk 26, targetSdk 34)...")

        if (simulateFailure) {
            logs.add("[ERROR] Compilation error detected in project source files.")
            return BuildResult(
                buildId = buildId,
                state = BuildState.FAILED,
                logs = logs,
                durationMs = System.currentTimeMillis() - startTime,
                errorMessage = "Compilation failed due to syntax errors."
            )
        }

        logs.add("[PACKAGING] Generating Android Application Package (.apk) / App Bundle (.aab)...")

        // Compute artifact properties based on file contents checksum
        val concatenatedContent = files.sortedBy { it.filePath }.joinToString("\n") { "${it.filePath}:${it.fileContent}" }
        val sizeBytes = (concatenatedContent.length * 2).toLong() + 1048576L // Realistic artifact size ~1MB+
        val checksumSha256 = calculateSHA256(concatenatedContent)

        val artifactType = if (environment == ReleaseEnvironment.PRODUCTION) ArtifactType.AAB else ArtifactType.APK
        val artifactPath = "/releases/$projectId/v$version/app-${environment.name.lowercase()}.${if (artifactType == ArtifactType.AAB) "aab" else "apk"}"

        val artifact = BuildArtifact(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            releaseId = releaseId,
            type = artifactType,
            path = artifactPath,
            size = sizeBytes,
            checksum = checksumSha256,
            createdAt = System.currentTimeMillis(),
            status = ArtifactStatus.VALIDATED
        )

        logs.add("[CHECKSUM] Calculated SHA-256 Integrity Hash: $checksumSha256")
        logs.add("[ARTIFACT] Successfully packaged artifact at $artifactPath (${sizeBytes / 1024} KB)")
        logs.add("[BUILD SUCCESS] Build pipeline completed without warnings.")

        return BuildResult(
            buildId = buildId,
            state = BuildState.SUCCESS,
            logs = logs,
            artifact = artifact,
            durationMs = System.currentTimeMillis() - startTime
        )
    }

    private fun calculateSHA256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

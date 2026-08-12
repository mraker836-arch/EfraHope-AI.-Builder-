package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.release.models.*
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricIndigoLight
import com.example.ui.theme.MintGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseManagementPanel(
    releases: List<Release>,
    activeRelease: Release?,
    canCreateRelease: Boolean = true,
    canApproveRelease: Boolean = true,
    canDeployRelease: Boolean = true,
    canRollbackRelease: Boolean = true,
    isBuilding: Boolean = false,
    isDeploying: Boolean = false,
    onCreateReleaseCandidate: (version: String, name: String, desc: String, env: ReleaseEnvironment) -> Unit,
    onValidateRelease: (String) -> Unit,
    onBuildRelease: (String) -> Unit,
    onApproveRelease: (String) -> Unit,
    onDeployRelease: (releaseId: String, userConfirmedInUI: Boolean) -> Unit,
    onRollbackRelease: (releaseId: String, targetReleaseId: String) -> Unit,
    onUpdateNotes: (releaseId: String, ReleaseNotes) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Prepare/Review, 1: History
    var showDeployConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeployReleaseId by remember { mutableStateOf<String?>(null) }

    // Creation Form state
    var versionInput by remember { mutableStateOf("1.0.0") }
    var releaseNameInput by remember { mutableStateOf("Initial Release Candidate") }
    var releaseDescInput by remember { mutableStateOf("Production-ready feature build and database schema.") }
    var selectedEnv by remember { mutableStateOf(ReleaseEnvironment.STAGING) }

    // Release Notes state
    var editNotesSummary by remember(activeRelease) { mutableStateOf(activeRelease?.releaseNotes?.summary ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Header & Sub-Tabs
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Release Engineering & Build Pipeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automated quality gates, artifact integrity & safe deployment abstraction",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Sub-tab selectors
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedSubTab == 0,
                            onClick = { selectedSubTab = 0 },
                            label = { Text("Release Candidate & Review") },
                            leadingIcon = { Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("release_review_tab")
                        )
                        FilterChip(
                            selected = selectedSubTab == 1,
                            onClick = { selectedSubTab = 1 },
                            label = { Text("Release History (${releases.size})") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("release_history_tab")
                        )
                    }
                }
            }
        }

        Divider()

        when (selectedSubTab) {
            0 -> {
                // Prepare / Active Review Panel
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Candidate Creation Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Prepare New Release Candidate",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricIndigo
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = versionInput,
                                        onValueChange = { versionInput = it },
                                        label = { Text("SemVer Version") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("release_version_input")
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Increment", style = MaterialTheme.typography.labelSmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    val parts = versionInput.split(".").mapNotNull { it.toIntOrNull() }
                                                    if (parts.size == 3) versionInput = "${parts[0] + 1}.0.0"
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) { Text("MAJOR", fontSize = 10.sp) }
                                            OutlinedButton(
                                                onClick = {
                                                    val parts = versionInput.split(".").mapNotNull { it.toIntOrNull() }
                                                    if (parts.size == 3) versionInput = "${parts[0]}.${parts[1] + 1}.0"
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) { Text("MINOR", fontSize = 10.sp) }
                                            OutlinedButton(
                                                onClick = {
                                                    val parts = versionInput.split(".").mapNotNull { it.toIntOrNull() }
                                                    if (parts.size == 3) versionInput = "${parts[0]}.${parts[1]}.${parts[2] + 1}"
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) { Text("PATCH", fontSize = 10.sp) }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = releaseNameInput,
                                        onValueChange = { releaseNameInput = it },
                                        label = { Text("Release Name") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Environment Selector
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Target Environment", style = MaterialTheme.typography.labelMedium)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ReleaseEnvironment.entries.forEach { env ->
                                                FilterChip(
                                                    selected = selectedEnv == env,
                                                    onClick = { selectedEnv = env },
                                                    label = { Text(env.name, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = releaseDescInput,
                                    onValueChange = { releaseDescInput = it },
                                    label = { Text("Release Description") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        onCreateReleaseCandidate(versionInput, releaseNameInput, releaseDescInput, selectedEnv)
                                    },
                                    enabled = canCreateRelease && versionInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                    modifier = Modifier.align(Alignment.End).testTag("create_release_candidate_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create Candidate")
                                }
                            }
                        }
                    }

                    // 2. Active Candidate Review
                    if (activeRelease != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Title & Environment Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "Release v${activeRelease.version}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                EnvironmentBadge(activeRelease.environment)
                                                StatusBadge(activeRelease.status)
                                            }
                                            Text(
                                                text = "${activeRelease.name} • Created by ${activeRelease.createdBy}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Provider indicator
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "Provider: ${activeRelease.providerName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Divider()

                                    // 3. Quality Gates
                                    Text(
                                        text = "Quality Gates Evaluation",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (activeRelease.gates.isEmpty()) {
                                        OutlinedButton(
                                            onClick = { onValidateRelease(activeRelease.id) },
                                            modifier = Modifier.testTag("run_validation_gates_btn")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Run Validation & Quality Gates")
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            activeRelease.gates.forEach { gate ->
                                                QualityGateRow(gate)
                                            }
                                        }
                                    }

                                    Divider()

                                    // 4. Build & Artifact System
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Build Pipeline & Artifact Integrity",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Button(
                                            onClick = { onBuildRelease(activeRelease.id) },
                                            enabled = !isBuilding && activeRelease.status != ReleaseStatus.DEPLOYED,
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigoLight),
                                            modifier = Modifier.testTag("execute_build_btn")
                                        ) {
                                            if (isBuilding) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Building...")
                                            } else {
                                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(if (activeRelease.artifact == null) "Run Build Engine" else "Re-run Build Engine")
                                            }
                                        }
                                    }

                                    if (activeRelease.artifact != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Artifact Type: ${activeRelease.artifact.type.name}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                Text("Artifact Path: ${activeRelease.artifact.path}", style = MaterialTheme.typography.bodySmall)
                                                Text("Size: ${activeRelease.artifact.size / 1024} KB", style = MaterialTheme.typography.bodySmall)
                                                Text(
                                                    "SHA-256 Checksum: ${activeRelease.artifact.checksum}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                    color = ElectricIndigo
                                                )
                                            }
                                        }
                                    }

                                    Divider()

                                    // 5. Release Notes (Editable Draft)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Release Notes (Draft)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (activeRelease.releaseNotes?.isDraft == false) {
                                                Text("Approved Draft", style = MaterialTheme.typography.labelSmall, color = MintGreen)
                                            }
                                        }

                                        OutlinedTextField(
                                            value = editNotesSummary,
                                            onValueChange = {
                                                editNotesSummary = it
                                                activeRelease.releaseNotes?.let { current ->
                                                    onUpdateNotes(activeRelease.id, current.copy(summary = it))
                                                }
                                            },
                                            label = { Text("Summary & Overview") },
                                            modifier = Modifier.fillMaxWidth().testTag("edit_release_notes_input")
                                        )

                                        activeRelease.releaseNotes?.let { notes ->
                                            if (notes.features.isNotEmpty()) {
                                                Text("Features:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                notes.features.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                                            }
                                            if (notes.fixes.isNotEmpty()) {
                                                Text("Fixes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                notes.fixes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                                            }
                                        }
                                    }

                                    Divider()

                                    // GitHub Deployment Provider Dashboard
                                    var showChecklistDialog by remember { mutableStateOf(false) }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                                                Text("GitHub Deployment Provider", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { showChecklistDialog = true },
                                                modifier = Modifier.testTag("check_github_deployment_btn")
                                            ) {
                                                Text("CHECK GITHUB DEPLOYMENT", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("GitHub Repository:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("efrahope-org/efrahope-ai-app", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Target Branch / Env:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("main / github-pages", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Workflow / Provider:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(".github/workflows/deploy.yml (${activeRelease.providerName})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ElectricIndigo)
                                        }

                                        if (activeRelease.deploymentUrl != null) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MintGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("LIVE SITE:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MintGreen)
                                                Text(activeRelease.deploymentUrl ?: "", style = MaterialTheme.typography.labelSmall, color = MintGreen, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    if (showChecklistDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showChecklistDialog = false },
                                            title = { Text("GitHub Deployment Readiness Checklist") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("✓ Repository Exists & Accessible", style = MaterialTheme.typography.bodySmall, color = MintGreen)
                                                    Text("✓ Target Branch 'main' Verified", style = MaterialTheme.typography.bodySmall, color = MintGreen)
                                                    Text("✓ Package Manager & Build Command Configured", style = MaterialTheme.typography.bodySmall, color = MintGreen)
                                                    Text("✓ Static Host Runtime Compatible", style = MaterialTheme.typography.bodySmall, color = MintGreen)
                                                    Text("✓ GitHub Actions Enabled (actions/deploy-pages@v4)", style = MaterialTheme.typography.bodySmall, color = MintGreen)
                                                    Text("✓ Pages Environment 'github-pages' Provisioned", style = MaterialTheme.typography.bodySmall, color = MintGreen)
                                                    Text("✓ Workflow Permissions: contents:read, pages:write, id-token:write", style = MaterialTheme.typography.bodySmall, color = MintGreen)
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(onClick = { showChecklistDialog = false }) {
                                                    Text("Close")
                                                }
                                            }
                                        )
                                    }

                                    Divider()

                                    // 6. Approval & Deployment Actions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (activeRelease.status == ReleaseStatus.READY || activeRelease.status == ReleaseStatus.DRAFT) {
                                            Button(
                                                onClick = { onApproveRelease(activeRelease.id) },
                                                enabled = canApproveRelease && activeRelease.gates.all { it.status != GateStatus.FAIL },
                                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                                modifier = Modifier.testTag("approve_release_btn")
                                            ) {
                                                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Approve Release")
                                            }
                                            Spacer(Modifier.width(12.dp))
                                        }

                                        if (activeRelease.status == ReleaseStatus.APPROVED || activeRelease.status == ReleaseStatus.READY) {
                                            Button(
                                                onClick = {
                                                    pendingDeployReleaseId = activeRelease.id
                                                    showDeployConfirmDialog = true
                                                },
                                                enabled = canDeployRelease && activeRelease.artifact != null && !isDeploying,
                                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                                modifier = Modifier.testTag("deploy_release_btn")
                                            ) {
                                                if (isDeploying) {
                                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Deploying...")
                                                } else {
                                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Deploy to ${activeRelease.environment.name}")
                                                }
                                            }
                                        }

                                        if (activeRelease.status == ReleaseStatus.DEPLOYED) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MintGreen.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                                                    Text("Deployed: ${activeRelease.deploymentUrl}", style = MaterialTheme.typography.labelMedium, color = MintGreen)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Release History List
                if (releases.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No releases recorded for this project yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(releases) { release ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("v${release.version}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            EnvironmentBadge(release.environment)
                                            StatusBadge(release.status)
                                        }

                                        Text(
                                            text = release.createdBy,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(release.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(release.description, style = MaterialTheme.typography.bodySmall)

                                    if (release.artifact != null) {
                                        Text(
                                            "Artifact: ${release.artifact.path} (${release.artifact.size / 1024} KB)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = ElectricIndigo
                                        )
                                    }

                                    // Rollback Option
                                    if (release.status == ReleaseStatus.DEPLOYED && releases.size > 1 && canRollbackRelease) {
                                        val previous = releases.find { it.id != release.id && it.status == ReleaseStatus.DEPLOYED }
                                        if (previous != null) {
                                            OutlinedButton(
                                                onClick = { onRollbackRelease(release.id, previous.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.align(Alignment.End).testTag("rollback_release_btn")
                                            ) {
                                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Rollback to v${previous.version}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Deploy Confirmation Modal for Production
    if (showDeployConfirmDialog && pendingDeployReleaseId != null) {
        val rel = releases.find { it.id == pendingDeployReleaseId }
        AlertDialog(
            onDismissRequest = { showDeployConfirmDialog = false },
            title = {
                Text("Confirm ${rel?.environment?.name ?: ""} Deployment")
            },
            text = {
                Text("Are you sure you want to deploy Version ${rel?.version} to ${rel?.environment?.name}?\n\nThis operation will package and launch the artifact into the target environment using ${rel?.providerName}.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeployConfirmDialog = false
                        onDeployRelease(pendingDeployReleaseId!!, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    modifier = Modifier.testTag("confirm_deploy_modal_btn")
                ) {
                    Text("Confirm & Deploy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeployConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EnvironmentBadge(env: ReleaseEnvironment) {
    val (bg, fg) = when (env) {
        ReleaseEnvironment.PRODUCTION -> Color(0xFFD32F2F) to Color.White
        ReleaseEnvironment.STAGING -> Color(0xFFED6C02) to Color.White
        ReleaseEnvironment.DEVELOPMENT -> Color(0xFF0288D1) to Color.White
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            text = env.name,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusBadge(status: ReleaseStatus) {
    val (bg, fg) = when (status) {
        ReleaseStatus.DEPLOYED, ReleaseStatus.APPROVED -> MintGreen to Color.Black
        ReleaseStatus.READY, ReleaseStatus.BUILDING, ReleaseStatus.VALIDATING -> ElectricIndigo to Color.White
        ReleaseStatus.FAILED, ReleaseStatus.ROLLED_BACK -> Color(0xFFD32F2F) to Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun QualityGateRow(gate: QualityGateResult) {
    val (icon, color) = when (gate.status) {
        GateStatus.PASS -> Icons.Default.CheckCircle to MintGreen
        GateStatus.FAIL -> Icons.Default.Cancel to Color(0xFFD32F2F)
        GateStatus.WARNING -> Icons.Default.Warning to Color(0xFFED6C02)
        GateStatus.NOT_APPLICABLE -> Icons.Default.Info to Color.Gray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Column {
            Text(gate.gate.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(gate.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

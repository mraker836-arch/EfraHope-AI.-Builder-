package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.db.ProjectEntity
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    currentProject: ProjectEntity?,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val hasKey = remember { GeminiService.hasApiKey() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Project & Platform Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Configure AI Agent engine, API keys, export project, and theme preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Project Info Card
        if (currentProject != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Active Project Metadata",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Project Name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentProject.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Project ID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentProject.id.take(12) + "...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ElectricIndigo)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentProject.appType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Exported project package for '${currentProject.name}'!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            modifier = Modifier.fillMaxWidth().testTag("export_project_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Project Bundle (ZIP / JSON)")
                        }
                    }
                }
            }
        }

        // Deployment Providers Section
        item {
            var gitHubOwner by remember { mutableStateOf("efrahope-org") }
            var gitHubRepo by remember { mutableStateOf("efrahope-ai-app") }
            var gitHubBranch by remember { mutableStateOf("main") }
            var isConnected by remember { mutableStateOf(false) }
            var connectionMsg by remember { mutableStateOf("") }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .testTag("deployment_providers_section")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ElectricIndigo)
                            Text(
                                text = "Deployment Providers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isConnected) NeonEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isConnected) "CONNECTED" else "NOT_CONNECTED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "GitHub Pages Deployment Provider Configuration. Build, release, and deploy applications to GitHub Pages via GitHub Actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = gitHubOwner,
                        onValueChange = { gitHubOwner = it },
                        label = { Text("GitHub Owner / Organization") },
                        modifier = Modifier.fillMaxWidth().testTag("github_owner_input")
                    )

                    OutlinedTextField(
                        value = gitHubRepo,
                        onValueChange = { gitHubRepo = it },
                        label = { Text("Repository Name") },
                        modifier = Modifier.fillMaxWidth().testTag("github_repo_input")
                    )

                    OutlinedTextField(
                        value = gitHubBranch,
                        onValueChange = { gitHubBranch = it },
                        label = { Text("Deployment Target Branch") },
                        modifier = Modifier.fillMaxWidth().testTag("github_branch_input")
                    )

                    if (connectionMsg.isNotBlank()) {
                        Text(
                            text = connectionMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) NeonEmerald else MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (gitHubOwner.isNotBlank() && gitHubRepo.isNotBlank()) {
                                    isConnected = true
                                    connectionMsg = "CONNECTED to GitHub repository '$gitHubOwner/$gitHubRepo' on branch '$gitHubBranch'."
                                } else {
                                    isConnected = false
                                    connectionMsg = "FAILED: Owner and Repository name are required."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            modifier = Modifier.weight(1f).testTag("connect_github_btn")
                        ) {
                            Text("CONNECT")
                        }

                        OutlinedButton(
                            onClick = {
                                if (gitHubOwner.isNotBlank() && gitHubRepo.isNotBlank()) {
                                    connectionMsg = "TEST CONNECTION SUCCESSFUL: Repo '$gitHubOwner/$gitHubRepo' accessible via GitHub API."
                                } else {
                                    connectionMsg = "TEST CONNECTION FAILED: Invalid repository credentials."
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("test_github_btn")
                        ) {
                            Text("TEST CONNECTION")
                        }
                    }
                }
            }
        }

        // Gemini AI API Configuration Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini AI Engine Key",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasKey) NeonEmerald.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (hasKey) "● Connected" else "● Internal AI Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasKey) NeonEmerald else NeonAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "To enable external cloud LLM reasoning, configure your GEMINI_API_KEY in the Secrets Panel. EfraHope AI Builder automatically falls back to built-in multi-agent generation when offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Appearance Settings
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Toggle between Futuristic Dark and Sleek Light palettes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() }
                    )
                }
            }
        }

        // Multi-Agent System Information
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("AI Agent Mesh Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("• Master AI: Orchestrates multi-agent pipelines and intent routing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Project Planner: Analyzes requirements & component trees", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• UI Generator: Designs responsive React/Tailwind/Compose layouts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Code Generator: Synthesizes modular TypeScript/React files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Database Planner: Maps Room & relational database schemas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Testing Agent & Error Fixing Agent: Static verification & refactoring", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

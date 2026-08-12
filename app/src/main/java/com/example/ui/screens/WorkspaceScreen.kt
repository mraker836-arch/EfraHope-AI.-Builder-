package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.models.ChangePlan
import com.example.data.ai.models.PlannerResult
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ProjectEntity
import com.example.data.db.ProjectFileEntity
import com.example.data.models.AIOperationState
import com.example.data.models.AppError
import com.example.data.models.TreeNode
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.PreviewDevice

@Composable
fun WorkspaceScreen(
    project: ProjectEntity?,
    files: List<ProjectFileEntity>,
    treeRoot: TreeNode.Folder,
    selectedFileId: String?,
    fileSearchQuery: String,
    unsavedFileContents: Map<String, String>,
    chatMessages: List<ChatMessageEntity>,
    previewDevice: PreviewDevice,
    isFullScreenPreview: Boolean,
    aiOperationState: AIOperationState = AIOperationState.IDLE,
    activePlan: PlannerResult? = null,
    activeChangePlan: ChangePlan? = null,
    errors: List<AppError> = emptyList(),
    onFileSearchQueryChange: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onAddFile: (path: String, content: String, language: String?) -> Unit,
    onRenameFile: (fileId: String, newPath: String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onUpdateUnsavedContent: (fileId: String, newContent: String, originalContent: String) -> Unit,
    onSaveFileContent: (fileId: String, content: String) -> Unit,
    onSaveAllFiles: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onApprovePlan: () -> Unit = {},
    onRegeneratePlan: () -> Unit = {},
    onApplyChangePlan: () -> Unit = {},
    onRejectChangePlan: () -> Unit = {},
    onCancelChangePlan: () -> Unit = {},
    onRollback: () -> Unit = {},
    onTogglePreviewDevice: (PreviewDevice) -> Unit,
    onToggleFullScreenPreview: (Boolean) -> Unit,
    onResolveError: (String) -> Unit = {},
    previewState: com.example.data.preview.PreviewState = com.example.data.preview.PreviewState(),
    onRefreshPreview: () -> Unit = {},
    onStopPreview: () -> Unit = {},
    onSetViewportMode: (com.example.data.preview.ViewportMode) -> Unit = {},
    databaseSchema: com.example.data.db.schema.DatabaseSchema? = null,
    apiContract: com.example.data.api.APIRouteContract? = null,
    databaseHealthStatus: com.example.data.db.schema.DatabaseHealthStatus = com.example.data.db.schema.DatabaseHealthStatus.DEVELOPMENT,
    environmentConfig: com.example.data.api.EnvironmentConfig = com.example.data.api.EnvironmentConfig(),
    schemaChangePlan: com.example.data.db.schema.SchemaChangePlan? = null,
    onApproveSchemaChangePlan: (com.example.data.db.schema.SchemaChangePlan) -> Unit = {},
    onRejectSchemaChangePlan: () -> Unit = {},
    userRole: com.example.data.auth.models.ProjectRole = com.example.data.auth.models.ProjectRole.OWNER,
    isReadOnly: Boolean = false,
    projectVersions: List<com.example.data.version.models.ProjectVersion> = emptyList(),
    projectMembers: List<com.example.data.auth.models.ProjectMember> = emptyList(),
    onCompareVersions: (String, String) -> com.example.data.version.models.VersionComparisonResult? = { _, _ -> null },
    onGetFileHistory: (String) -> List<com.example.data.version.models.FileVersionRecord> = { emptyList() },
    onPreviewRollback: (String) -> com.example.data.version.models.RollbackPreview? = { null },
    onExecuteRollback: (String) -> Unit = {},
    onAddMember: (String, com.example.data.auth.models.ProjectRole) -> Unit = { _, _ -> },
    onUpdateRole: (String, com.example.data.auth.models.ProjectRole) -> Unit = { _, _ -> },
    onRemoveMember: (String) -> Unit = {},
    isRestoring: Boolean = false,
    releases: List<com.example.data.release.models.Release> = emptyList(),
    activeRelease: com.example.data.release.models.Release? = null,
    canCreateRelease: Boolean = true,
    canApproveRelease: Boolean = true,
    canDeployRelease: Boolean = true,
    canRollbackRelease: Boolean = true,
    isBuildingRelease: Boolean = false,
    isDeployingRelease: Boolean = false,
    onCreateReleaseCandidate: (version: String, name: String, desc: String, env: com.example.data.release.models.ReleaseEnvironment) -> Unit = { _, _, _, _ -> },
    onValidateRelease: (String) -> Unit = {},
    onBuildRelease: (String) -> Unit = {},
    onApproveRelease: (String) -> Unit = {},
    onDeployRelease: (String, Boolean) -> Unit = { _, _ -> },
    onRollbackRelease: (String, String) -> Unit = { _, _ -> },
    onUpdateReleaseNotes: (String, com.example.data.release.models.ReleaseNotes) -> Unit = { _, _ -> }
) {
    if (project == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("No active project selected. Please open or create a project from Dashboard.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val selectedFile = files.find { it.fileId == selectedFileId } ?: files.firstOrNull()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 840

    var activeCenterTab by remember { mutableStateOf(0) } // 0: Live Preview, 1: Code Editor
    var mobileSectionTab by remember { mutableStateOf(1) } // 0: Files, 1: Center Canvas, 2: AI Assistant
    var showErrorPanel by remember { mutableStateOf(false) }

    val unresolvedErrors = errors.filter { !it.resolved }
    val unsavedCount = unsavedFileContents.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("workspace_screen")
    ) {
        if (isReadOnly) {
            Surface(
                color = AmberWarning.copy(alpha = 0.2f),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("read_only_mode_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VIEWER MODE ($userRole) — Project is read-only. Editing code, creating files, AI modifications, and database schema changes are disabled.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Top Workspace Control Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = ElectricIndigo) {
                        Text(
                            text = project.appType,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Save All Unsaved Indicator
                    if (unsavedCount > 0) {
                        Surface(
                            onClick = onSaveAllFiles,
                            shape = RoundedCornerShape(6.dp),
                            color = NeonAmber.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("●", color = NeonAmber, fontSize = 10.sp)
                                Text(
                                    text = "Save All ($unsavedCount)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                            }
                        }
                    }

                    // AI Operation State Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (aiOperationState) {
                            AIOperationState.IDLE -> NeonEmerald.copy(alpha = 0.2f)
                            AIOperationState.ERROR -> NeonRose.copy(alpha = 0.2f)
                            else -> ElectricIndigo.copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (aiOperationState != AIOperationState.IDLE && aiOperationState != AIOperationState.ERROR) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    color = ElectricIndigo,
                                    strokeWidth = 1.5.dp
                                )
                            }
                            Text(
                                text = "AI: ${aiOperationState.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (aiOperationState) {
                                    AIOperationState.IDLE -> NeonEmerald
                                    AIOperationState.ERROR -> NeonRose
                                    else -> ElectricIndigo
                                }
                            )
                        }
                    }

                    // Error Notification Indicator
                    if (unresolvedErrors.isNotEmpty()) {
                        Surface(
                            onClick = { showErrorPanel = !showErrorPanel },
                            shape = RoundedCornerShape(6.dp),
                            color = NeonRose.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = NeonRose,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${unresolvedErrors.size} Issue${if (unresolvedErrors.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonRose
                                )
                            }
                        }
                    }
                }

                // Center View Mode Switcher (Preview vs Code)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { activeCenterTab = 0 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 0) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("preview_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (activeCenterTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Live Preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { activeCenterTab = 1 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 1) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("code_editor_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = if (activeCenterTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Code Editor",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { activeCenterTab = 2 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 2) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("split_view_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewColumn,
                                contentDescription = null,
                                tint = if (activeCenterTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Split",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { activeCenterTab = 3 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 3) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("error_center_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = if (activeCenterTab == 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Error Center",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { activeCenterTab = 4 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 4) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("testing_dashboard_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = if (activeCenterTab == 4) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Testing",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 4) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { activeCenterTab = 5 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 5) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("database_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (activeCenterTab == 5) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Database",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 5) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { activeCenterTab = 6 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 6) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("history_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = if (activeCenterTab == 6) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 6) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { activeCenterTab = 7 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 7) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("members_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = if (activeCenterTab == 7) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Members",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 7) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { activeCenterTab = 8 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCenterTab == 8) ElectricIndigo else Color.Transparent,
                        modifier = Modifier.testTag("releases_tab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = if (activeCenterTab == 8) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Releases",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCenterTab == 8) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Error Panel Overlay if expanded
        AnimatedVisibility(visible = showErrorPanel && unresolvedErrors.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, NeonRose, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Project Diagnostics & Errors (${unresolvedErrors.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonRose
                        )
                        IconButton(onClick = { showErrorPanel = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                        }
                    }
                    unresolvedErrors.take(3).forEach { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• [${err.source}] ${err.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onResolveError(err.id) }) {
                                Text("Resolve", fontSize = 11.sp, color = NeonEmerald)
                            }
                        }
                    }
                }
            }
        }

        // Main Three-Panel Area
        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT PANEL: File Tree (25% width)
                ProjectTree(
                    treeRoot = treeRoot,
                    files = files,
                    selectedFileId = selectedFile?.fileId,
                    fileSearchQuery = fileSearchQuery,
                    onSearchQueryChange = onFileSearchQueryChange,
                    onSelectFile = { id ->
                        onSelectFile(id)
                        activeCenterTab = 1
                    },
                    onToggleFolder = onToggleFolder,
                    onCreateFolder = onCreateFolder,
                    onCreateFile = onAddFile,
                    onRenameFile = onRenameFile,
                    onDeleteFile = onDeleteFile,
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                )

                // CENTER PANEL: Preview or Code Editor (50% width)
                Box(
                    modifier = Modifier
                        .weight(0.50f)
                        .fillMaxHeight()
                ) {
                    when (activeCenterTab) {
                        0 -> LivePreview(
                            projectName = project.name,
                            files = files,
                            previewState = previewState,
                            onRefresh = onRefreshPreview,
                            onStop = onStopPreview,
                            onSetViewport = onSetViewportMode,
                            onToggleFitToScreen = {},
                            onOpenErrorCenter = { activeCenterTab = 3 },
                            isFullScreen = isFullScreenPreview,
                            onToggleFullScreen = onToggleFullScreenPreview,
                            modifier = Modifier.fillMaxSize()
                        )
                        1 -> CodeEditor(
                            file = selectedFile,
                            unsavedContent = selectedFile?.fileId?.let { unsavedFileContents[it] },
                            onUpdateUnsavedContent = onUpdateUnsavedContent,
                            onSaveContent = onSaveFileContent,
                            onSaveAll = onSaveAllFiles,
                            onAskAI = onSendChatMessage,
                            modifier = Modifier.fillMaxSize()
                        )
                        2 -> Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CodeEditor(
                                file = selectedFile,
                                unsavedContent = selectedFile?.fileId?.let { unsavedFileContents[it] },
                                onUpdateUnsavedContent = onUpdateUnsavedContent,
                                onSaveContent = onSaveFileContent,
                                onSaveAll = onSaveAllFiles,
                                onAskAI = onSendChatMessage,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            LivePreview(
                                projectName = project.name,
                                files = files,
                                previewState = previewState,
                                onRefresh = onRefreshPreview,
                                onStop = onStopPreview,
                                onSetViewport = onSetViewportMode,
                                onToggleFitToScreen = {},
                                onOpenErrorCenter = { activeCenterTab = 3 },
                                isFullScreen = isFullScreenPreview,
                                onToggleFullScreen = onToggleFullScreenPreview,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        3 -> ErrorCenterView(
                            errors = errors.filter { it.severity == com.example.data.models.ErrorSeverity.ERROR },
                            warnings = errors.filter { it.severity == com.example.data.models.ErrorSeverity.WARNING || it.severity == com.example.data.models.ErrorSeverity.INFO },
                            health = com.example.data.models.ProjectHealth.HEALTHY,
                            autoFixEnabled = false,
                            onToggleAutoFix = {},
                            onRunValidation = {},
                            onAnalyzeError = {},
                            onProposeFix = {},
                            onResolveError = onResolveError,
                            modifier = Modifier.fillMaxSize()
                        )
                        4 -> TestingDashboard(
                            project = com.example.data.models.ProjectModel(
                                name = project.name,
                                description = project.description,
                                appType = project.appType,
                                errors = errors.filter { it.severity == com.example.data.models.ErrorSeverity.ERROR },
                                warnings = errors.filter { it.severity == com.example.data.models.ErrorSeverity.WARNING }
                            ),
                            health = com.example.data.models.ProjectHealth.HEALTHY,
                            autoFixEnabled = false,
                            onToggleAutoFix = {},
                            onRunValidation = {},
                            onRunTests = {},
                            onAnalyzeErrors = {},
                            onFixErrors = {},
                            modifier = Modifier.fillMaxSize()
                        )
                        5 -> DatabasePlanView(
                            schema = databaseSchema,
                            apiContract = apiContract,
                            healthStatus = databaseHealthStatus,
                            envConfig = environmentConfig,
                            activeChangePlan = schemaChangePlan,
                            onApproveChangePlan = onApproveSchemaChangePlan,
                            onRejectChangePlan = onRejectSchemaChangePlan,
                            modifier = Modifier.fillMaxSize()
                        )
                        6 -> VersionHistoryPanel(
                            versions = projectVersions,
                            selectedFilePaths = files.map { it.filePath },
                            onCompareVersions = onCompareVersions,
                            onGetFileHistory = onGetFileHistory,
                            onPreviewRollback = onPreviewRollback,
                            onExecuteRollback = onExecuteRollback,
                            canRestore = !isReadOnly && userRole == com.example.data.auth.models.ProjectRole.OWNER,
                            isRestoring = isRestoring,
                            modifier = Modifier.fillMaxSize()
                        )
                        7 -> ProjectMembersPanel(
                            members = projectMembers,
                            canShare = !isReadOnly && userRole == com.example.data.auth.models.ProjectRole.OWNER,
                            onAddMember = onAddMember,
                            onUpdateRole = onUpdateRole,
                            onRemoveMember = onRemoveMember,
                            modifier = Modifier.fillMaxSize()
                        )
                        8 -> ReleaseManagementPanel(
                            releases = releases,
                            activeRelease = activeRelease,
                            canCreateRelease = canCreateRelease && !isReadOnly,
                            canApproveRelease = canApproveRelease && !isReadOnly,
                            canDeployRelease = canDeployRelease && !isReadOnly,
                            canRollbackRelease = canRollbackRelease && !isReadOnly,
                            isBuilding = isBuildingRelease,
                            isDeploying = isDeployingRelease,
                            onCreateReleaseCandidate = onCreateReleaseCandidate,
                            onValidateRelease = onValidateRelease,
                            onBuildRelease = onBuildRelease,
                            onApproveRelease = onApproveRelease,
                            onDeployRelease = onDeployRelease,
                            onRollbackRelease = onRollbackRelease,
                            onUpdateNotes = onUpdateReleaseNotes,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // RIGHT PANEL: AI Chat Assistant (25% width)
                AIChatPanel(
                    messages = chatMessages,
                    onSendMessage = onSendChatMessage,
                    activePlan = activePlan,
                    activeChangePlan = activeChangePlan,
                    aiOperationState = aiOperationState,
                    onApprovePlan = onApprovePlan,
                    onRegeneratePlan = onRegeneratePlan,
                    onApplyChangePlan = onApplyChangePlan,
                    onRejectChangePlan = onRejectChangePlan,
                    onCancelChangePlan = onCancelChangePlan,
                    onRollback = onRollback,
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                )
            }
        } else {
            // Mobile / Narrow Screen Layout with Panel Tabs
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Mobile Section Bar
                TabRow(
                    selectedTabIndex = mobileSectionTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Tab(
                        selected = mobileSectionTab == 0,
                        onClick = { mobileSectionTab = 0 },
                        text = { Text("1. Files (${files.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = mobileSectionTab == 1,
                        onClick = { mobileSectionTab = 1 },
                        text = { Text(if (activeCenterTab == 0) "2. Preview" else "2. Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = mobileSectionTab == 2,
                        onClick = { mobileSectionTab = 2 },
                        text = { Text("3. AI Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (mobileSectionTab) {
                        0 -> ProjectTree(
                            treeRoot = treeRoot,
                            files = files,
                            selectedFileId = selectedFile?.fileId,
                            fileSearchQuery = fileSearchQuery,
                            onSearchQueryChange = onFileSearchQueryChange,
                            onSelectFile = { id ->
                                onSelectFile(id)
                                activeCenterTab = 1
                                mobileSectionTab = 1
                            },
                            onToggleFolder = onToggleFolder,
                            onCreateFolder = onCreateFolder,
                            onCreateFile = onAddFile,
                            onRenameFile = onRenameFile,
                            onDeleteFile = onDeleteFile,
                            modifier = Modifier.fillMaxSize()
                        )
                        1 -> if (activeCenterTab == 0) {
                            LivePreview(
                                projectName = project.name,
                                files = files,
                                previewState = previewState,
                                onRefresh = onRefreshPreview,
                                onStop = onStopPreview,
                                onSetViewport = onSetViewportMode,
                                onToggleFitToScreen = {},
                                onOpenErrorCenter = { activeCenterTab = 3 },
                                isFullScreen = isFullScreenPreview,
                                onToggleFullScreen = onToggleFullScreenPreview,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CodeEditor(
                                file = selectedFile,
                                unsavedContent = selectedFile?.fileId?.let { unsavedFileContents[it] },
                                onUpdateUnsavedContent = onUpdateUnsavedContent,
                                onSaveContent = onSaveFileContent,
                                onSaveAll = onSaveAllFiles,
                                onAskAI = onSendChatMessage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        2 -> AIChatPanel(
                            messages = chatMessages,
                            onSendMessage = onSendChatMessage,
                            activePlan = activePlan,
                            activeChangePlan = activeChangePlan,
                            aiOperationState = aiOperationState,
                            onApprovePlan = onApprovePlan,
                            onRegeneratePlan = onRegeneratePlan,
                            onApplyChangePlan = onApplyChangePlan,
                            onRejectChangePlan = onRejectChangePlan,
                            onCancelChangePlan = onCancelChangePlan,
                            onRollback = onRollback,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

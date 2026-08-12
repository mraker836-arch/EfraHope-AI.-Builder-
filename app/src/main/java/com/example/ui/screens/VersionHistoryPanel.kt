package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.version.models.*
import com.example.ui.components.RestorePreviewModal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryPanel(
    versions: List<ProjectVersion>,
    selectedFilePaths: List<String>,
    onCompareVersions: (String, String) -> VersionComparisonResult?,
    onGetFileHistory: (String) -> List<FileVersionRecord>,
    onPreviewRollback: (String) -> RollbackPreview?,
    onExecuteRollback: (String) -> Unit,
    canRestore: Boolean,
    isRestoring: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: History & Timeline, 1: Compare, 2: File History
    var rollbackPreviewTarget by remember { mutableStateOf<RollbackPreview?>(null) }

    // State for Compare tab
    var compareVerAId by remember { mutableStateOf(versions.getOrNull(1)?.id ?: versions.firstOrNull()?.id ?: "") }
    var compareVerBId by remember { mutableStateOf(versions.firstOrNull()?.id ?: "") }

    // State for File History tab
    var selectedInspectFile by remember { mutableStateOf(selectedFilePaths.firstOrNull() ?: "") }

    if (rollbackPreviewTarget != null) {
        RestorePreviewModal(
            preview = rollbackPreviewTarget!!,
            onConfirmRestore = {
                val targetId = rollbackPreviewTarget!!.targetVersion.id
                rollbackPreviewTarget = null
                onExecuteRollback(targetId)
            },
            onDismiss = { rollbackPreviewTarget = null },
            isLoading = isRestoring
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Versions & Timeline") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Compare") },
                    icon = { Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("File History") },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> VersionsTimelineTab(
                    versions = versions,
                    canRestore = canRestore,
                    onCompareClick = { ver ->
                        compareVerBId = ver.id
                        val verA = versions.find { it.versionNumber == ver.versionNumber - 1 } ?: ver
                        compareVerAId = verA.id
                        selectedTab = 1
                    },
                    onRestoreClick = { ver ->
                        rollbackPreviewTarget = onPreviewRollback(ver.id)
                    }
                )

                1 -> CompareVersionsTab(
                    versions = versions,
                    verAId = compareVerAId,
                    verBId = compareVerBId,
                    onVerAChanged = { compareVerAId = it },
                    onVerBChanged = { compareVerBId = it },
                    onCompare = onCompareVersions
                )

                2 -> FileHistoryTab(
                    filePaths = selectedFilePaths,
                    selectedFilePath = selectedInspectFile,
                    onFilePathSelected = { selectedInspectFile = it },
                    onGetHistory = onGetFileHistory
                )
            }
        }
    }
}

@Composable
fun VersionsTimelineTab(
    versions: List<ProjectVersion>,
    canRestore: Boolean,
    onCompareClick: (ProjectVersion) -> Unit,
    onRestoreClick: (ProjectVersion) -> Unit
) {
    if (versions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No version history recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(versions) { version ->
            VersionCardItem(
                version = version,
                canRestore = canRestore,
                onCompareClick = { onCompareClick(version) },
                onRestoreClick = { onRestoreClick(version) }
            )
        }
    }
}

@Composable
fun VersionCardItem(
    version: ProjectVersion,
    canRestore: Boolean,
    onCompareClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(version.createdAt) { dateFormat.format(Date(version.createdAt)) }

    val sourceColor = when (version.source) {
        VersionSource.USER_CHANGE -> Color(0xFF10B981) // Emerald
        VersionSource.AI_CHANGE -> Color(0xFF6366F1)   // Indigo
        VersionSource.IMPORT -> Color(0xFF3B82F6)      // Blue
        VersionSource.RESTORE -> Color(0xFFF59E0B)     // Amber
        VersionSource.SYSTEM -> Color(0xFF6B7280)      // Gray
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (version.status == VersionStatus.ACTIVE)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (version.status == VersionStatus.ACTIVE) 1.5.dp else 0.5.dp,
                color = if (version.status == VersionStatus.ACTIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "v${version.versionNumber}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = version.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = sourceColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = version.source.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = sourceColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (version.status == VersionStatus.ACTIVE) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (version.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = version.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (version.aiUserPrompt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Prompt: \"${version.aiUserPrompt}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "By ${version.createdBy} • $formattedDate • ${version.changedFilesCount} files changed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    OutlinedButton(
                        onClick = onCompareClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compare", style = MaterialTheme.typography.labelSmall)
                    }

                    if (version.status != VersionStatus.ACTIVE && canRestore) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRestoreClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompareVersionsTab(
    versions: List<ProjectVersion>,
    verAId: String,
    verBId: String,
    onVerAChanged: (String) -> Unit,
    onVerBChanged: (String) -> Unit,
    onCompare: (String, String) -> VersionComparisonResult?
) {
    val comparison = remember(verAId, verBId) {
        if (verAId.isNotBlank() && verBId.isNotBlank() && verAId != verBId) {
            onCompare(verAId, verBId)
        } else null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Base Version (A)", style = MaterialTheme.typography.labelMedium)
                DropdownVersionSelector(
                    versions = versions,
                    selectedId = verAId,
                    onSelected = onVerAChanged
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Target Version (B)", style = MaterialTheme.typography.labelMedium)
                DropdownVersionSelector(
                    versions = versions,
                    selectedId = verBId,
                    onSelected = onVerBChanged
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (comparison == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Select two distinct versions above to compare.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text("Added: ${comparison.addedFiles.size}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Modified: ${comparison.modifiedFiles.size}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                            Text("Removed: ${comparison.removedFiles.size}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (comparison.schemaDiff != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Database Schema Change", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(comparison.schemaDiff.summary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                items(comparison.fileDiffs) { diff ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = diff.filePath,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "+${diff.additions} / -${diff.deletions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (diff.newContent.length > 500) diff.newContent.take(500) + "\n... (truncated)" else diff.newContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFF8FAFC)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileHistoryTab(
    filePaths: List<String>,
    selectedFilePath: String,
    onFilePathSelected: (String) -> Unit,
    onGetHistory: (String) -> List<FileVersionRecord>
) {
    var history by remember(selectedFilePath) {
        mutableStateOf(if (selectedFilePath.isNotBlank()) onGetHistory(selectedFilePath) else emptyList())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Select File to Inspect", style = MaterialTheme.typography.labelMedium)
        DropdownFileSelector(
            filePaths = filePaths,
            selectedPath = selectedFilePath,
            onSelected = {
                onFilePathSelected(it)
                history = onGetHistory(it)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFilePath.isBlank() || history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Select a file above to view its version history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history) { record ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "v${record.versionNumber} — ${record.versionLabel}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = record.source.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (record.diffFromPrevious != null) {
                                Text(
                                    text = record.diffFromPrevious,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (record.content.length > 300) record.content.take(300) + "\n..." else record.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownVersionSelector(
    versions: List<ProjectVersion>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVer = versions.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedVer?.let { "v${it.versionNumber} - ${it.label}" } ?: "Select version",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            versions.forEach { ver ->
                DropdownMenuItem(
                    text = { Text("v${ver.versionNumber} - ${ver.label}") },
                    onClick = {
                        onSelected(ver.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFileSelector(
    filePaths: List<String>,
    selectedPath: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedPath.ifBlank { "Select file" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filePaths.forEach { path ->
                DropdownMenuItem(
                    text = { Text(path) },
                    onClick = {
                        onSelected(path)
                        expanded = false
                    }
                )
            }
        }
    }
}

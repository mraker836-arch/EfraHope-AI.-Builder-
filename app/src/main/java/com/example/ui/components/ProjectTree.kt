package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ProjectFileEntity
import com.example.data.models.TreeNode
import com.example.data.utils.PathSecurity
import com.example.ui.theme.*

@Composable
fun ProjectTree(
    treeRoot: TreeNode.Folder,
    files: List<ProjectFileEntity>,
    selectedFileId: String?,
    fileSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onCreateFile: (path: String, content: String, language: String?) -> Unit,
    onCreateFolder: (folderPath: String) -> Unit,
    onRenameFile: (fileId: String, newPath: String) -> Unit,
    onDeleteFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddFileDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<TreeNode.File?>(null) }

    var newFilePathText by remember { mutableStateOf("") }
    var newFolderPathText by remember { mutableStateOf("") }
    var renamePathText by remember { mutableStateOf("") }
    var pathErrorMessage by remember { mutableStateOf<String?>(null) }

    // Add File Dialog
    if (showAddFileDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddFileDialog = false
                pathErrorMessage = null
            },
            title = { Text("Create New Project File", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter relative file path (e.g., 'src/components/Header.tsx'):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newFilePathText,
                        onValueChange = {
                            newFilePathText = it
                            pathErrorMessage = null
                        },
                        placeholder = { Text("src/pages/Dashboard.tsx") },
                        singleLine = true,
                        isError = pathErrorMessage != null,
                        modifier = Modifier.fillMaxWidth().testTag("new_file_path_input")
                    )
                    pathErrorMessage?.let { err ->
                        Text(text = err, color = NeonRose, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validation = PathSecurity.validatePath(newFilePathText)
                        if (validation.isSuccess) {
                            val cleanPath = validation.getOrThrow()
                            val lang = PathSecurity.detectLanguage(cleanPath)
                            val defaultContent = when (lang) {
                                "typescript" -> "// Created via EfraHope AI Builder\nimport React from 'react';\n\nexport default function Component() {\n  return <div>Component</div>;\n}"
                                "html" -> "<!DOCTYPE html>\n<html>\n<head><title>Page</title></head>\n<body>\n  <h1>New Page</h1>\n</body>\n</html>"
                                "css" -> "/* Custom Styles */\n.container {\n  padding: 16px;\n}"
                                "json" -> "{\n  \"name\": \"app-config\",\n  \"version\": \"1.0.0\"\n}"
                                else -> "// New file in EfraHope AI Builder"
                            }
                            onCreateFile(cleanPath, defaultContent, lang)
                            newFilePathText = ""
                            showAddFileDialog = false
                            pathErrorMessage = null
                        } else {
                            pathErrorMessage = validation.exceptionOrNull()?.message
                        }
                    }
                ) {
                    Text("Create File")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddFileDialog = false
                        pathErrorMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Folder Dialog
    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddFolderDialog = false
                pathErrorMessage = null
            },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter folder path (e.g., 'src/services/api'):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newFolderPathText,
                        onValueChange = {
                            newFolderPathText = it
                            pathErrorMessage = null
                        },
                        placeholder = { Text("src/hooks") },
                        singleLine = true,
                        isError = pathErrorMessage != null,
                        modifier = Modifier.fillMaxWidth().testTag("new_folder_path_input")
                    )
                    pathErrorMessage?.let { err ->
                        Text(text = err, color = NeonRose, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validation = PathSecurity.validatePath(newFolderPathText)
                        if (validation.isSuccess) {
                            onCreateFolder(validation.getOrThrow())
                            newFolderPathText = ""
                            showAddFolderDialog = false
                            pathErrorMessage = null
                        } else {
                            pathErrorMessage = validation.exceptionOrNull()?.message
                        }
                    }
                ) {
                    Text("Create Folder")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddFolderDialog = false
                        pathErrorMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename File Dialog
    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = {
                fileToRename = null
                pathErrorMessage = null
            },
            title = { Text("Rename File", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter new relative path for file:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = renamePathText,
                        onValueChange = {
                            renamePathText = it
                            pathErrorMessage = null
                        },
                        singleLine = true,
                        isError = pathErrorMessage != null,
                        modifier = Modifier.fillMaxWidth().testTag("rename_file_path_input")
                    )
                    pathErrorMessage?.let { err ->
                        Text(text = err, color = NeonRose, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validation = PathSecurity.validatePath(renamePathText)
                        if (validation.isSuccess) {
                            fileToRename?.fileId?.let { id ->
                                onRenameFile(id, validation.getOrThrow())
                            }
                            fileToRename = null
                            pathErrorMessage = null
                        } else {
                            pathErrorMessage = validation.exceptionOrNull()?.message
                        }
                    }
                ) {
                    Text("Save Rename")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        fileToRename = null
                        pathErrorMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = ElectricIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "EXPLORER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { showAddFolderDialog = true },
                        modifier = Modifier.size(28.dp).testTag("add_folder_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Create Folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { showAddFileDialog = true },
                        modifier = Modifier.size(28.dp).testTag("add_file_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = "Add File",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quick Filter Bar
            OutlinedTextField(
                value = fileSearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Filter files...", fontSize = 11.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                trailingIcon = {
                    if (fileSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricIndigo,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .height(38.dp)
                    .testTag("file_filter_input")
            )

            Divider(color = MaterialTheme.colorScheme.outline)

            // Dynamic Folder / File Tree List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                if (fileSearchQuery.isNotBlank()) {
                    // Filtered list view mode
                    val filteredFiles = files.filter { it.filePath.contains(fileSearchQuery, ignoreCase = true) }
                    items(filteredFiles, key = { it.fileId }) { file ->
                        FileTreeRow(
                            file = TreeNode.File(
                                name = PathSecurity.getFileName(file.filePath),
                                path = file.filePath,
                                fileId = file.fileId,
                                language = file.language,
                                isMain = file.isMain
                            ),
                            depth = 0,
                            isSelected = file.fileId == selectedFileId,
                            canDelete = files.size > 1 && !file.isMain,
                            onSelectFile = onSelectFile,
                            onRenameFile = {
                                fileToRename = TreeNode.File(
                                    name = PathSecurity.getFileName(file.filePath),
                                    path = file.filePath,
                                    fileId = file.fileId,
                                    language = file.language
                                )
                                renamePathText = file.filePath
                            },
                            onDeleteFile = onDeleteFile
                        )
                    }
                } else {
                    // Full hierarchical tree mode
                    renderTreeNodes(
                        folder = treeRoot,
                        depth = 0,
                        selectedFileId = selectedFileId,
                        fileCount = files.size,
                        onSelectFile = onSelectFile,
                        onToggleFolder = onToggleFolder,
                        onRenameFile = { f ->
                            fileToRename = f
                            renamePathText = f.path
                        },
                        onDeleteFile = onDeleteFile
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderTreeNodes(
    folder: TreeNode.Folder,
    depth: Int,
    selectedFileId: String?,
    fileCount: Int,
    onSelectFile: (String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onRenameFile: (TreeNode.File) -> Unit,
    onDeleteFile: (String) -> Unit
) {
    if (folder.path.isNotEmpty()) {
        item(key = "folder_${folder.path}") {
            FolderTreeRow(
                folder = folder,
                depth = depth,
                onToggleFolder = onToggleFolder
            )
        }
    }

    if (folder.path.isEmpty() || folder.isExpanded) {
        val childDepth = if (folder.path.isEmpty()) 0 else depth + 1
        folder.children.forEach { child ->
            when (child) {
                is TreeNode.Folder -> {
                    renderTreeNodes(
                        folder = child,
                        depth = childDepth,
                        selectedFileId = selectedFileId,
                        fileCount = fileCount,
                        onSelectFile = onSelectFile,
                        onToggleFolder = onToggleFolder,
                        onRenameFile = onRenameFile,
                        onDeleteFile = onDeleteFile
                    )
                }
                is TreeNode.File -> {
                    item(key = "file_${child.fileId}") {
                        FileTreeRow(
                            file = child,
                            depth = childDepth,
                            isSelected = child.fileId == selectedFileId,
                            canDelete = fileCount > 1 && !child.isMain,
                            onSelectFile = onSelectFile,
                            onRenameFile = { onRenameFile(child) },
                            onDeleteFile = onDeleteFile
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTreeRow(
    folder: TreeNode.Folder,
    depth: Int,
    onToggleFolder: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleFolder(folder.path) }
            .padding(start = (depth * 12 + 8).dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
            .testTag("folder_item_${folder.path}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (folder.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )

        Icon(
            imageVector = if (folder.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
            contentDescription = null,
            tint = ElectricIndigo,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FileTreeRow(
    file: TreeNode.File,
    depth: Int,
    isSelected: Boolean,
    canDelete: Boolean,
    onSelectFile: (String) -> Unit,
    onRenameFile: () -> Unit,
    onDeleteFile: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onSelectFile(file.fileId) }
            .padding(start = (depth * 12 + 16).dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
            .testTag("file_item_${file.fileId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val fileIcon = when {
                file.name.endsWith(".html") -> Icons.Default.Code
                file.name.endsWith(".tsx") || file.name.endsWith(".ts") -> Icons.Default.IntegrationInstructions
                file.name.endsWith(".css") -> Icons.Default.Style
                file.name.endsWith(".json") -> Icons.Default.DataObject
                else -> Icons.Default.InsertDriveFile
            }

            Icon(
                imageVector = fileIcon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else CyberCyan,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            if (file.isModified) {
                Text(
                    text = "●",
                    color = NeonAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        showMenu = false
                        onRenameFile()
                    }
                )
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = NeonRose) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRose, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onDeleteFile(file.fileId)
                        }
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import com.example.data.models.*
import com.example.ui.theme.*

private val SlateGray = Color(0xFF64748B)

@Composable
fun ErrorCenterView(
    errors: List<AppError>,
    warnings: List<AppError>,
    health: ProjectHealth,
    autoFixEnabled: Boolean,
    onToggleAutoFix: (Boolean) -> Unit,
    onRunValidation: () -> Unit,
    onAnalyzeError: (AppError) -> Unit,
    onProposeFix: (AppError) -> Unit,
    onResolveError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSeverityFilter by remember { mutableStateOf<ErrorSeverity?>(null) }
    var selectedTypeFilter by remember { mutableStateOf<ErrorType?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<ErrorStatus?>(null) }
    var selectedError by remember { mutableStateOf<AppError?>(null) }

    val allIssues = remember(errors, warnings) { errors + warnings }
    val filteredIssues = remember(allIssues, selectedSeverityFilter, selectedTypeFilter, selectedStatusFilter) {
        allIssues.filter { err ->
            (selectedSeverityFilter == null || err.severity == selectedSeverityFilter) &&
            (selectedTypeFilter == null || err.type == selectedTypeFilter) &&
            (selectedStatusFilter == null || err.status == selectedStatusFilter)
        }
    }

    val totalErrorsCount = errors.size
    val totalWarningsCount = warnings.size
    val openErrorsCount = errors.count { it.status == ErrorStatus.OPEN || !it.resolved }
    val fixedCount = allIssues.count { it.status == ErrorStatus.FIXED || it.resolved }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxSize()
            .testTag("error_center_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar with Summary Metrics & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = ElectricIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Error Center & Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (health) {
                            ProjectHealth.HEALTHY -> NeonEmerald.copy(alpha = 0.2f)
                            ProjectHealth.WARNING -> NeonAmber.copy(alpha = 0.2f)
                            ProjectHealth.CRITICAL -> NeonRose.copy(alpha = 0.2f)
                            ProjectHealth.UNKNOWN -> SlateGray.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = health.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (health) {
                                ProjectHealth.HEALTHY -> NeonEmerald
                                ProjectHealth.WARNING -> NeonAmber
                                ProjectHealth.CRITICAL -> NeonRose
                                ProjectHealth.UNKNOWN -> SlateGray
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Auto-fix Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Auto-Fix",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = autoFixEnabled,
                            onCheckedChange = onToggleAutoFix,
                            modifier = Modifier.testTag("auto_fix_switch")
                        )
                    }

                    Button(
                        onClick = onRunValidation,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("run_validation_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Re-validate", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Metric Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip("Open Errors", "$openErrorsCount", NeonRose, Modifier.weight(1f))
                MetricChip("Total Errors", "$totalErrorsCount", NeonRose.copy(alpha = 0.8f), Modifier.weight(1f))
                MetricChip("Warnings", "$totalWarningsCount", NeonAmber, Modifier.weight(1f))
                MetricChip("Fixed", "$fixedCount", NeonEmerald, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            // Filter Controls Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Filters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterBadge("Severity: ${selectedSeverityFilter?.name ?: "ALL"}") {
                        selectedSeverityFilter = when (selectedSeverityFilter) {
                            null -> ErrorSeverity.ERROR
                            ErrorSeverity.ERROR -> ErrorSeverity.WARNING
                            ErrorSeverity.WARNING -> ErrorSeverity.INFO
                            else -> null
                        }
                    }
                    FilterBadge("Type: ${selectedTypeFilter?.name ?: "ALL"}") {
                        selectedTypeFilter = when (selectedTypeFilter) {
                            null -> ErrorType.IMPORT
                            ErrorType.IMPORT -> ErrorType.SYNTAX
                            ErrorType.SYNTAX -> ErrorType.TYPE
                            ErrorType.TYPE -> ErrorType.COMPONENT
                            ErrorType.COMPONENT -> ErrorType.BUILD
                            else -> null
                        }
                    }
                    FilterBadge("Status: ${selectedStatusFilter?.name ?: "ALL"}") {
                        selectedStatusFilter = when (selectedStatusFilter) {
                            null -> ErrorStatus.OPEN
                            ErrorStatus.OPEN -> ErrorStatus.FIX_PROPOSED
                            ErrorStatus.FIX_PROPOSED -> ErrorStatus.FIXED
                            else -> null
                        }
                    }
                    if (selectedSeverityFilter != null || selectedTypeFilter != null || selectedStatusFilter != null) {
                        TextButton(
                            onClick = {
                                selectedSeverityFilter = null
                                selectedTypeFilter = null
                                selectedStatusFilter = null
                            }
                        ) {
                            Text("Reset", fontSize = 11.sp, color = ElectricIndigo)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Main Content Area: Master-Detail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Error List
                LazyColumn(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredIssues.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No issues detected for selected filters.", color = NeonEmerald)
                            }
                        }
                    } else {
                        items(filteredIssues) { issue ->
                            ErrorListItem(
                                issue = issue,
                                isSelected = selectedError?.id == issue.id,
                                onClick = { selectedError = issue },
                                onQuickFix = { onProposeFix(issue) }
                            )
                        }
                    }
                }

                // Right: Selected Error Details
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    val issue = selectedError ?: filteredIssues.firstOrNull()
                    if (issue != null) {
                        ErrorDetailsPane(
                            issue = issue,
                            onAnalyze = { onAnalyzeError(issue) },
                            onProposeFix = { onProposeFix(issue) },
                            onResolve = { onResolveError(issue.id) }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Select an error to view root cause analysis and details.", color = SlateGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilterBadge(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ErrorListItem(
    issue: AppError,
    isSelected: Boolean,
    onClick: () -> Unit,
    onQuickFix: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) ElectricIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) BorderStroke(1.dp, ElectricIndigo) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (issue.severity == ErrorSeverity.ERROR) Icons.Default.Error else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (issue.severity == ErrorSeverity.ERROR) NeonRose else NeonAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = issue.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricIndigo
                    )
                    Text(
                        text = "• ${issue.status.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = issue.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                if (issue.file != null) {
                    Text(
                        text = "${issue.file}${if (issue.line != null) ":${issue.line}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = SlateGray
                    )
                }
            }

            if (issue.status != ErrorStatus.FIXED) {
                IconButton(onClick = onQuickFix) {
                    Icon(Icons.Default.Build, contentDescription = "Propose Fix", tint = NeonEmerald, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ErrorDetailsPane(
    issue: AppError,
    onAnalyze: () -> Unit,
    onProposeFix: () -> Unit,
    onResolve: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Issue Details & Analysis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricIndigo
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = RoundedCornerShape(4.dp), color = NeonRose.copy(alpha = 0.2f)) {
                    Text(issue.type.name, fontSize = 10.sp, color = NeonRose, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                }
                Surface(shape = RoundedCornerShape(4.dp), color = NeonAmber.copy(alpha = 0.2f)) {
                    Text(issue.severity.name, fontSize = 10.sp, color = NeonAmber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                }
                Surface(shape = RoundedCornerShape(4.dp), color = ElectricIndigo.copy(alpha = 0.2f)) {
                    Text(issue.source, fontSize = 10.sp, color = ElectricIndigo, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                }
            }

            Text(
                text = issue.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (issue.file != null) {
                Text(
                    text = "File: ${issue.file}${if (issue.line != null) " (Line ${issue.line}${if (issue.column != null) ", Col ${issue.column}" else ""})" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = SlateGray
                )
            }

            if (issue.code != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = issue.code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NeonRose,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Divider()

            Text("Root Cause & Diagnosis:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                text = issue.possibleCause ?: "Analyzing structural references and code dependencies...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text("Suggested Fix Strategy:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                text = issue.suggestedSolution ?: "Run AI Error Fixing Agent to generate a targeted ChangePlan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (issue.relatedFiles.isNotEmpty()) {
                Text("Related Files:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                issue.relatedFiles.forEach { rf ->
                    Text("• $rf", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SlateGray)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAnalyze,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Text("ANALYZE ERROR", fontSize = 11.sp)
            }
            Button(
                onClick = onProposeFix,
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                modifier = Modifier.weight(1f)
            ) {
                Text("PROPOSE FIX", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

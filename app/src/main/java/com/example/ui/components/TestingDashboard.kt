package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SlateGray = Color(0xFF64748B)

@Composable
fun TestingDashboard(
    project: ProjectModel,
    health: ProjectHealth,
    autoFixEnabled: Boolean,
    onToggleAutoFix: (Boolean) -> Unit,
    onRunValidation: () -> Unit,
    onRunTests: () -> Unit,
    onAnalyzeErrors: () -> Unit,
    onFixErrors: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalErrors = project.errors.size
    val totalWarnings = project.warnings.size
    val openErrors = project.errors.count { !it.resolved && it.status != ErrorStatus.FIXED }
    val fixedErrors = project.errors.count { it.resolved || it.status == ErrorStatus.FIXED }

    val lastHistory = project.validationHistory.lastOrNull()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxSize()
            .testTag("testing_dashboard")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Dashboard Header Bar
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
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Testing & Quality Dashboard",
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
                            text = "HEALTH: ${health.name}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (health) {
                                ProjectHealth.HEALTHY -> NeonEmerald
                                ProjectHealth.WARNING -> NeonAmber
                                ProjectHealth.CRITICAL -> NeonRose
                                ProjectHealth.UNKNOWN -> SlateGray
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Auto-Fix Controlled Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (autoFixEnabled) "AUTO-FIX: ON (Safe Mode)" else "AUTO-FIX: OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (autoFixEnabled) NeonEmerald else SlateGray
                    )
                    Switch(
                        checked = autoFixEnabled,
                        onCheckedChange = onToggleAutoFix,
                        modifier = Modifier.testTag("testing_autofix_switch")
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRunValidation,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("run_validation_action_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("RUN VALIDATION", fontSize = 11.sp)
                }

                Button(
                    onClick = onRunTests,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("RUN TESTS", fontSize = 11.sp)
                }

                Button(
                    onClick = onAnalyzeErrors,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ANALYZE ERRORS", fontSize = 11.sp, color = Color.Black)
                }

                Button(
                    onClick = onFixErrors,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("FIX ERRORS", fontSize = 11.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 8 Metric Status Cards Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusMetricCard(
                        title = "BUILD STATUS",
                        value = project.buildStatus,
                        subtext = "Compiler check",
                        isSuccess = project.buildStatus != "FAILED" && openErrors == 0,
                        modifier = Modifier.weight(1f)
                    )
                    StatusMetricCard(
                        title = "TEST STATUS",
                        value = if (openErrors == 0) "PASSED" else "FAILED",
                        subtext = "Static analysis",
                        isSuccess = openErrors == 0,
                        modifier = Modifier.weight(1f)
                    )
                    StatusMetricCard(
                        title = "TYPE CHECK",
                        value = if (project.errors.none { it.type == ErrorType.TYPE }) "VALID" else "ISSUES",
                        subtext = "TypeScript references",
                        isSuccess = project.errors.none { it.type == ErrorType.TYPE },
                        modifier = Modifier.weight(1f)
                    )
                    StatusMetricCard(
                        title = "LINT STATUS",
                        value = if (totalWarnings == 0) "CLEAN" else "$totalWarnings WARNS",
                        subtext = "Code styling",
                        isSuccess = totalWarnings == 0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusMetricCard(
                        title = "ERROR COUNT",
                        value = "$openErrors Open",
                        subtext = "$totalErrors Total logged",
                        isSuccess = openErrors == 0,
                        modifier = Modifier.weight(1f)
                    )
                    StatusMetricCard(
                        title = "WARNING COUNT",
                        value = "$totalWarnings",
                        subtext = "Non-blocking issues",
                        isSuccess = true,
                        modifier = Modifier.weight(1f)
                    )
                    StatusMetricCard(
                        title = "LAST VALIDATION",
                        value = if (lastHistory != null) dateFormat.format(Date(lastHistory.timestamp)) else "N/A",
                        subtext = "${lastHistory?.durationMs ?: 0} ms duration",
                        isSuccess = lastHistory?.isSuccess ?: true,
                        modifier = Modifier.weight(1f)
                    )
                    StatusMetricCard(
                        title = "FIXED ERRORS",
                        value = "$fixedErrors Fixed",
                        subtext = "Resolved by AI / User",
                        isSuccess = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Validation History Log Section
            Text(
                text = "Validation Execution History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricIndigo
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (project.validationHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No validation history recorded yet. Run validation to generate logs.", color = SlateGray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(project.validationHistory.reversed()) { record ->
                            ValidationHistoryRow(record = record, dateFormat = dateFormat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMetricCard(
    title: String,
    value: String,
    subtext: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSuccess) NeonEmerald.copy(alpha = 0.3f) else NeonRose.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) NeonEmerald else NeonRose
            )
            Text(subtext, style = MaterialTheme.typography.labelSmall, color = SlateGray, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ValidationHistoryRow(
    record: ValidationHistoryRecord,
    dateFormat: SimpleDateFormat
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (record.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (record.isSuccess) NeonEmerald else NeonRose,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "[${record.validationType}] Triggered by ${record.triggeredBy}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Errors: ${record.errorsCount} | Fixed: ${record.fixedErrorsCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${record.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = SlateGray
                )
                Text(
                    text = dateFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGray
                )
            }
        }
    }
}

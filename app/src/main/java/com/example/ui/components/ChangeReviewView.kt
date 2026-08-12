package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
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
import com.example.data.ai.models.ChangeOperation
import com.example.data.ai.models.ChangePlan
import com.example.data.ai.models.FileDiff

@Composable
fun ChangeReviewView(
    plan: ChangePlan,
    onApply: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHighRiskDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Summary, 1: Code Diff

    val containsDeletion = plan.changes.any { it.operation == ChangeOperation.DELETE_FILE }
    val isHighRisk = plan.riskLevel.equals("High", ignoreCase = true) || containsDeletion

    val totalAdditions = plan.diffs.sumOf { it.additions }
    val totalDeletions = plan.diffs.sumOf { it.deletions }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Change Plan",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change Review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                RiskBadge(riskLevel = if (containsDeletion) "High" else plan.riskLevel)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Intent & Summary
            Text(
                text = "Intent: ${plan.intent} • ${plan.summary}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(10.dp)
            ) {
                Text(
                    text = plan.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diff Metrics Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${plan.affectedFiles.size} affected files",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+$totalAdditions lines",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80)
                    )
                    Text(
                        text = "-$totalDeletions lines",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector (Files vs Diff View)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF38BDF8)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Affected Files (${plan.changes.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Code Diffs", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                if (selectedTab == 0) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(plan.changes) { change ->
                            FileChangeRow(change = change)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(plan.diffs) { diff ->
                            FileDiffCard(diff = diff)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (APPLY, REJECT, CANCEL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF87171)))
                ) {
                    Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (isHighRisk) {
                            showHighRiskDialog = true
                        } else {
                            onApply()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // High Risk Confirmation Dialog
    if (showHighRiskDialog) {
        AlertDialog(
            onDismissRequest = { showHighRiskDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFF87171)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("High Risk Operation", color = Color.White)
                }
            },
            text = {
                Text(
                    "This change contains file deletions or high-risk architectural updates. Are you sure you want to apply these changes?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHighRiskDialog = false
                        onApply()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHighRiskDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
fun RiskBadge(riskLevel: String) {
    val (bgColor, textColor) = when (riskLevel.lowercase()) {
        "high" -> Pair(Color(0xFF7F1D1D), Color(0xFFFCA5A5))
        "medium" -> Pair(Color(0xFF78350F), Color(0xFFFDE68A))
        else -> Pair(Color(0xFF14532D), Color(0xFF86EFAC))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$riskLevel Risk",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FileChangeRow(change: com.example.data.ai.models.FileChange) {
    val (opColor, opText) = when (change.operation) {
        ChangeOperation.CREATE_FILE -> Pair(Color(0xFF16A34A), "CREATE")
        ChangeOperation.UPDATE_FILE -> Pair(Color(0xFF0284C7), "UPDATE")
        ChangeOperation.DELETE_FILE -> Pair(Color(0xFFDC2626), "DELETE")
        ChangeOperation.RENAME_FILE -> Pair(Color(0xFFD97706), "RENAME")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F172A))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Feed,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = change.targetFilePath,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(opColor)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = opText,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FileDiffCard(diff: FileDiff) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(
            text = diff.filePath,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF020617))
                .padding(6.dp)
        ) {
            val lines = diff.newContent.lines().take(10)
            Column {
                lines.forEach { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF86EFAC)
                    )
                }
                if (diff.newContent.lines().size > 10) {
                    Text(
                        text = "... (${diff.newContent.lines().size - 10} more lines)",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

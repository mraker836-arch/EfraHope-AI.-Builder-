package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.ProjectFileEntity
import com.example.data.preview.PreviewState
import com.example.data.preview.PreviewStatus
import com.example.data.preview.ViewportMode
import com.example.ui.theme.*

@Composable
fun LivePreview(
    projectName: String,
    files: List<ProjectFileEntity>,
    previewState: PreviewState,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onSetViewport: (ViewportMode) -> Unit,
    onToggleFitToScreen: () -> Unit,
    onOpenErrorCenter: () -> Unit,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val previewContent = @Composable {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Preview Toolbar Bar
            PreviewToolbar(
                previewState = previewState,
                onRefresh = onRefresh,
                onStop = onStop,
                onSetViewport = onSetViewport,
                onToggleFitToScreen = onToggleFitToScreen,
                onToggleFullScreen = { onToggleFullScreen(!isFullScreen) },
                onOpenErrorCenter = onOpenErrorCenter
            )

            Divider(color = MaterialTheme.colorScheme.outline)

            // Canvas Sandbox Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020617))
                    .padding(
                        when (previewState.viewportMode) {
                            ViewportMode.DESKTOP -> 0.dp
                            ViewportMode.TABLET -> 16.dp
                            ViewportMode.MOBILE -> 24.dp
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val canvasModifier = when (previewState.viewportMode) {
                    ViewportMode.DESKTOP -> Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    ViewportMode.TABLET -> Modifier
                        .width(768.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background)
                    ViewportMode.MOBILE -> Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.background)
                }

                when (previewState.status) {
                    PreviewStatus.PREPARING, PreviewStatus.BUILDING, PreviewStatus.STARTING -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator(
                                color = ElectricIndigo,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (previewState.status) {
                                    PreviewStatus.PREPARING -> "Preparing preview environment..."
                                    PreviewStatus.BUILDING -> "Building project assets..."
                                    else -> "Starting sandboxed runtime..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    PreviewStatus.ERROR -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Preview Error",
                                tint = NeonRose,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Preview Build/Runtime Error",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = previewState.runtimeErrors.firstOrNull()?.message
                                    ?: "Build validation failed. The preview cannot start until errors are resolved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onRefresh,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rebuild Preview")
                                }
                                OutlinedButton(onClick = onOpenErrorCenter) {
                                    Text("Open Error Center")
                                }
                            }
                        }
                    }

                    PreviewStatus.STOPPED -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircleOutline,
                                contentDescription = "Stopped",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Preview Stopped",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onRefresh) {
                                Text("Start Preview")
                            }
                        }
                    }

                    PreviewStatus.RUNNING, PreviewStatus.REFRESHING -> {
                        InteractiveAppCanvas(
                            projectName = projectName,
                            files = files,
                            modifier = canvasModifier
                        )
                    }

                    PreviewStatus.IDLE -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Button(onClick = onRefresh) {
                                Text("Launch Preview")
                            }
                        }
                    }
                }
            }
        }
    }

    if (isFullScreen) {
        Dialog(
            onDismissRequest = { onToggleFullScreen(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                previewContent()
            }
        }
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            previewContent()
        }
    }
}

@Composable
private fun InteractiveAppCanvas(
    projectName: String,
    files: List<ProjectFileEntity>,
    modifier: Modifier = Modifier
) {
    val isRiceTrading = files.any { it.fileContent.contains("Rice", ignoreCase = true) || it.fileContent.contains("Jasmine", ignoreCase = true) }
    var activeTab by remember { mutableStateOf("Dashboard") }

    // State for interactive elements
    var jasmineStock by remember { mutableStateOf(520) }
    var basmatiStock by remember { mutableStateOf(380) }
    var longGrainStock by remember { mutableStateOf(340) }
    var orderPlacedCount by remember { mutableStateOf(14) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Header Bar
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = if (isRiceTrading) "🌾" else "⚡", fontSize = 16.sp)
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Dashboard", "Inventory", "Orders").forEach { tab ->
                            Surface(
                                onClick = { activeTab = tab },
                                shape = RoundedCornerShape(6.dp),
                                color = if (activeTab == tab) ElectricIndigo else Color.Transparent
                            ) {
                                Text(
                                    text = tab,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == tab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeTab == "Dashboard") {
            item {
                Text(
                    text = if (isRiceTrading) "Executive Rice Trading Overview" else "Application Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricBox(
                        title = "Total Inventory",
                        value = "${jasmineStock + basmatiStock + longGrainStock} Tons",
                        color = NeonEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Bulk Orders",
                        value = "$orderPlacedCount Orders",
                        color = ElectricIndigo,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Live Stock Distribution", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        StockBar("Super Jasmine 5%", "$jasmineStock T", jasmineStock / 1000f, NeonEmerald)
                        StockBar("Royal Basmati 1121", "$basmatiStock T", basmatiStock / 1000f, ElectricIndigo)
                        StockBar("Long Grain White", "$longGrainStock T", longGrainStock / 1000f, CyberCyan)
                    }
                }
            }
        } else if (activeTab == "Inventory") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Inventory Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { jasmineStock += 50 },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+ Receive Batch", fontSize = 11.sp)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InventoryRow("Jasmine Super 5%", "Thailand", "$jasmineStock Tons", "$740 / Ton")
                    InventoryRow("Royal Basmati", "India", "$basmatiStock Tons", "$970 / Ton")
                    InventoryRow("Long Grain White", "Vietnam", "$longGrainStock Tons", "$580 / Ton")
                }
            }
        } else {
            item {
                Text("Trade Orders Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Button(
                    onClick = {
                        orderPlacedCount++
                        if (jasmineStock >= 20) jasmineStock -= 20
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("⚡ Simulate Customer Order Placement")
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrderRow("ORD-#$orderPlacedCount", "Global Grain Corp", "20 Tons Jasmine", "DISPATCHED")
                    OrderRow("ORD-#13", "Apex Food Importers", "40 Tons Basmati", "IN TRANSIT")
                }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun StockBar(label: String, qty: String, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
            Text(qty, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun InventoryRow(name: String, origin: String, stock: String, price: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(origin, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stock, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeonEmerald)
                Text(price, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OrderRow(id: String, client: String, items: String, status: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(id, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ElectricIndigo)
                Text(client, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(items, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(status, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

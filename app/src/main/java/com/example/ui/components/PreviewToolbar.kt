package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preview.PreviewState
import com.example.data.preview.PreviewStatus
import com.example.data.preview.ViewportMode
import com.example.ui.theme.*

@Composable
fun PreviewToolbar(
    previewState: PreviewState,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onSetViewport: (ViewportMode) -> Unit,
    onToggleFitToScreen: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onOpenErrorCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: URL & Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Window dots
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonRose))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonAmber))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonEmerald))

                Spacer(modifier = Modifier.width(4.dp))

                // URL pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = previewState.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Status Badge
                val (badgeColor, statusText) = when (previewState.status) {
                    PreviewStatus.IDLE -> Pair(Color.Gray, "Idle")
                    PreviewStatus.PREPARING -> Pair(NeonAmber, "Preparing preview...")
                    PreviewStatus.BUILDING -> Pair(NeonAmber, "Building...")
                    PreviewStatus.STARTING -> Pair(NeonAmber, "Starting...")
                    PreviewStatus.RUNNING -> Pair(NeonEmerald, "Running")
                    PreviewStatus.REFRESHING -> Pair(ElectricIndigo, "Refreshing...")
                    PreviewStatus.ERROR -> Pair(NeonRose, "Preview error")
                    PreviewStatus.STOPPED -> Pair(Color.Gray, "Stopped")
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.testTag("preview_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 10.sp
                        )
                    }
                }

                // Data Layer Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    modifier = Modifier.testTag("preview_data_provider_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ElectricIndigo)
                        )
                        Text(
                            text = "Development data provider active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricIndigo,
                            fontSize = 10.sp
                        )
                    }
                }

                // Error Indicator Button if errors exist
                if (previewState.runtimeErrors.isNotEmpty()) {
                    Surface(
                        onClick = onOpenErrorCenter,
                        shape = RoundedCornerShape(12.dp),
                        color = NeonRose,
                        modifier = Modifier.testTag("preview_error_indicator_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Errors",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${previewState.runtimeErrors.size} Errors",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Right: Toolbar Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Viewport Selection
                IconButton(
                    onClick = { onSetViewport(ViewportMode.DESKTOP) },
                    modifier = Modifier.size(28.dp).testTag("viewport_desktop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = "Desktop View",
                        tint = if (previewState.viewportMode == ViewportMode.DESKTOP) ElectricIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onSetViewport(ViewportMode.TABLET) },
                    modifier = Modifier.size(28.dp).testTag("viewport_tablet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tablet,
                        contentDescription = "Tablet View",
                        tint = if (previewState.viewportMode == ViewportMode.TABLET) ElectricIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onSetViewport(ViewportMode.MOBILE) },
                    modifier = Modifier.size(28.dp).testTag("viewport_mobile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Mobile View",
                        tint = if (previewState.viewportMode == ViewportMode.MOBILE) ElectricIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Fit / Actual Size Toggle
                IconButton(
                    onClick = onToggleFitToScreen,
                    modifier = Modifier.size(28.dp).testTag("fit_screen_button")
                ) {
                    Icon(
                        imageVector = if (previewState.isFitToScreen) Icons.Default.FitScreen else Icons.Default.AspectRatio,
                        contentDescription = "Toggle Aspect",
                        tint = if (previewState.isFitToScreen) ElectricIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Refresh Button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp).testTag("refresh_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Preview",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Stop Button
                IconButton(
                    onClick = onStop,
                    enabled = previewState.status == PreviewStatus.RUNNING || previewState.status == PreviewStatus.REFRESHING,
                    modifier = Modifier.size(28.dp).testTag("stop_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Preview",
                        tint = if (previewState.status == PreviewStatus.RUNNING) NeonRose else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Open Preview / Fullscreen
                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier.size(28.dp).testTag("fullscreen_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open Preview",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen

@Composable
fun Navbar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    currentUser: com.example.data.auth.models.User? = null,
    authState: com.example.data.auth.models.AuthState = com.example.data.auth.models.AuthState.AUTHENTICATED
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigate(AppScreen.LANDING) }
                    .testTag("brand_logo_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ElectricIndigo, CyberCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_1786233150559),
                        contentDescription = "EfraHope AI Logo",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column {
                    Text(
                        text = "EfraHope AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "BUILDER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        color = CyberCyan,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Navigation Links
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabButton(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentScreen == AppScreen.LANDING,
                    onClick = { onNavigate(AppScreen.LANDING) },
                    testTag = "nav_landing_button"
                )

                NavTabButton(
                    label = "Dashboard",
                    icon = Icons.Default.Dashboard,
                    isSelected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { onNavigate(AppScreen.DASHBOARD) },
                    testTag = "nav_dashboard_button"
                )

                NavTabButton(
                    label = "Workspace",
                    icon = Icons.Default.Code,
                    isSelected = currentScreen == AppScreen.WORKSPACE,
                    onClick = { onNavigate(AppScreen.WORKSPACE) },
                    testTag = "nav_workspace_button"
                )

                NavTabButton(
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    isSelected = currentScreen == AppScreen.SETTINGS,
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    testTag = "nav_settings_button"
                )

                if (authState == com.example.data.auth.models.AuthState.AUTHENTICATED && currentUser != null) {
                    NavTabButton(
                        label = currentUser.displayName.take(12),
                        icon = Icons.Default.AccountCircle,
                        isSelected = currentScreen == AppScreen.PROFILE,
                        onClick = { onNavigate(AppScreen.PROFILE) },
                        testTag = "nav_profile_button"
                    )
                } else {
                    NavTabButton(
                        label = "Sign In",
                        icon = Icons.Default.Lock,
                        isSelected = currentScreen == AppScreen.LOGIN,
                        onClick = { onNavigate(AppScreen.LOGIN) },
                        testTag = "nav_login_button"
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

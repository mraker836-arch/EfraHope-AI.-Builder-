package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CreateProjectScreen(
    initialName: String = "",
    initialDesc: String = "",
    initialType: String = "Rice Trading",
    onCreateProject: (name: String, desc: String, type: String, style: String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(initialName.ifEmpty { "Rice Trading Hub" }) }
    var description by remember {
        mutableStateOf(
            initialDesc.ifEmpty {
                "Build an online rice trading application with products, inventory, customers, orders and sales dashboard."
            }
        )
    }
    var appType by remember { mutableStateOf(initialType) }
    var preferredStyle by remember { mutableStateOf("Sleek Slate") }

    val appTypes = listOf("Rice Trading", "E-Commerce", "SaaS CRM", "Task Manager", "Custom App")
    val styles = listOf("Sleek Slate", "Modern Cyberpunk", "Vibrant Clean", "Minimal Dark")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("create_project_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Create New AI Application",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Project Name Input
                    Column {
                        Text(
                            text = "Project Name",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("e.g. Rice Trading Hub") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("project_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // Application Description Input
                    Column {
                        Text(
                            text = "Describe What You Want To Build",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Describe the application features, workflow, pages, database models...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("project_description_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Quick Prompts
                    Column {
                        Text(
                            text = "Example Prompt Ideas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PromptChip(
                                label = "🌾 Rice Trading App: Inventory, bulk orders, customers & sales dashboard",
                                onClick = {
                                    name = "Rice Trading Hub"
                                    description = "Build an online rice trading application with products, inventory, customers, orders and sales dashboard."
                                    appType = "Rice Trading"
                                }
                            )
                            PromptChip(
                                label = "🛍️ Next-Gen Storefront: Cyber headphones, shopping cart & instant checkout",
                                onClick = {
                                    name = "CyberGear Store"
                                    description = "Build a modern e-commerce web storefront with product catalog, cart drawer, ratings, and checkout."
                                    appType = "E-Commerce"
                                }
                            )
                        }
                    }

                    // Application Type Selection
                    Column {
                        Text(
                            text = "Application Category",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            appTypes.take(3).forEach { type ->
                                FilterChip(
                                    selected = appType == type,
                                    onClick = { appType = type },
                                    label = { Text(type) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            appTypes.drop(3).forEach { type ->
                                FilterChip(
                                    selected = appType == type,
                                    onClick = { appType = type },
                                    label = { Text(type) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Visual Style Selection
                    Column {
                        Text(
                            text = "Preferred Visual Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            styles.forEach { style ->
                                FilterChip(
                                    selected = preferredStyle == style,
                                    onClick = { preferredStyle = style },
                                    label = { Text(style, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreateProject(name, description, appType, preferredStyle)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_create_project_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Project & Launch AI Builder", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

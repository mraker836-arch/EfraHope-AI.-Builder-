package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.APIRouteContract
import com.example.data.api.EnvironmentConfig
import com.example.data.db.schema.*
import com.example.ui.theme.*

@Composable
fun DatabasePlanView(
    schema: DatabaseSchema?,
    apiContract: APIRouteContract?,
    healthStatus: DatabaseHealthStatus,
    envConfig: EnvironmentConfig,
    activeChangePlan: SchemaChangePlan? = null,
    onApproveChangePlan: (SchemaChangePlan) -> Unit = {},
    onRejectChangePlan: () -> Unit = {},
    onAddFieldToEntity: (String, String, DataType) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedEntityName by remember { mutableStateOf<String?>(null) }
    var showAddFieldDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Header & Database Health Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Database",
                        tint = ElectricIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Database & API Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Health Badge
                val (badgeColor, statusLabel) = when (healthStatus) {
                    DatabaseHealthStatus.NOT_CONFIGURED -> Pair(Color.Gray, "Not Configured")
                    DatabaseHealthStatus.DEVELOPMENT -> Pair(NeonEmerald, "Development Data Provider Active")
                    DatabaseHealthStatus.CONNECTED -> Pair(NeonEmerald, "Connected")
                    DatabaseHealthStatus.WARNING -> Pair(NeonAmber, "Warning")
                    DatabaseHealthStatus.ERROR -> Pair(NeonRose, "Error")
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.testTag("database_health_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(badgeColor))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            ) {
                val tabs = listOf("Overview", "Entities", "Relationships", "Indexes", "API Contracts", "Migrations", "Environment")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pending Change Plan Approval Banner if active
            if (activeChangePlan != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = ElectricIndigo)
                            Text(
                                text = "Pending Schema Change Plan Review",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeChangePlan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        activeChangePlan.changes.forEach { change ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ${change.type} ON ${change.entityName}.${change.targetName}", style = MaterialTheme.typography.labelSmall)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (change.riskLevel == ChangeRiskLevel.DESTRUCTIVE) NeonRose else NeonAmber
                                ) {
                                    Text(
                                        text = change.riskLevel.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApproveChangePlan(activeChangePlan) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Approve & Apply Schema")
                            }
                            OutlinedButton(
                                onClick = onRejectChangePlan,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Reject Change")
                            }
                        }
                    }
                }
            }

            if (schema == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Database Schema Generated Yet", style = MaterialTheme.typography.titleMedium)
                        Text("Use AI Prompt to generate a structured database architecture.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> OverviewTab(schema = schema, envConfig = envConfig)
                    1 -> EntitiesTab(schema = schema, onSelectEntity = { selectedEntityName = it })
                    2 -> RelationshipsTab(schema = schema)
                    3 -> IndexesTab(schema = schema)
                    4 -> APIContractsTab(apiContract = apiContract)
                    5 -> MigrationsTab(schema = schema)
                    6 -> EnvironmentTab(envConfig = envConfig)
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(schema: DatabaseSchema, envConfig: EnvironmentConfig) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Database System Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Provider Type:", style = MaterialTheme.typography.bodySmall)
                        Text(schema.providerType, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = ElectricIndigo)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Schema Version:", style = MaterialTheme.typography.bodySmall)
                        Text("v${schema.version}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Entities Count:", style = MaterialTheme.typography.bodySmall)
                        Text("${schema.entities.size} Tables", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = NeonEmerald)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Relationships Count:", style = MaterialTheme.typography.bodySmall)
                        Text("${schema.relationships.size} Foreign Keys", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text("Entities & Tables", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        items(schema.entities) { entity ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(entity.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(entity.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = ElectricIndigo.copy(alpha = 0.1f)) {
                        Text(
                            "${entity.fields.size} Fields",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricIndigo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntitiesTab(schema: DatabaseSchema, onSelectEntity: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(schema.entities) { entity ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                            Text(entity.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("PK: ${entity.primaryKey}", style = MaterialTheme.typography.labelSmall, color = NeonAmber, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(entity.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Fields (${entity.fields.size}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    entity.fields.forEach { field ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (field.isPrimaryKey) {
                                    Icon(Icons.Default.Key, contentDescription = "PK", tint = NeonAmber, modifier = Modifier.size(12.dp))
                                }
                                Text(field.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(
                                        field.type.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontSize = 9.sp
                                    )
                                }
                                if (field.unique) {
                                    Text("UNIQUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                }
                                if (field.nullable) {
                                    Text("NULLABLE", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipsTab(schema: DatabaseSchema) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (schema.relationships.isEmpty()) {
            item {
                Text("No relationships defined in schema.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            items(schema.relationships) { rel ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(rel.fromEntity, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = ElectricIndigo)
                                Text(rel.toEntity, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Foreign Key: ${rel.foreignKey}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = ElectricIndigo.copy(alpha = 0.15f)) {
                            Text(
                                rel.type.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricIndigo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexesTab(schema: DatabaseSchema) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (schema.indexes.isEmpty()) {
            item {
                Text("No custom indexes defined in schema.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            items(schema.indexes) { idx ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(idx.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Table: ${idx.tableName} | Fields: ${idx.fields.joinToString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (idx.isUnique) {
                            Surface(shape = RoundedCornerShape(4.dp), color = CyberCyan.copy(alpha = 0.2f)) {
                                Text("UNIQUE", style = MaterialTheme.typography.labelSmall, color = CyberCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun APIContractsTab(apiContract: APIRouteContract?) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (apiContract == null || apiContract.endpoints.isEmpty()) {
            item {
                Text("No API contracts generated.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            items(apiContract.endpoints) { ep ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val methodColor = when (ep.method) {
                                com.example.data.api.HTTPMethod.GET -> NeonEmerald
                                com.example.data.api.HTTPMethod.POST -> ElectricIndigo
                                com.example.data.api.HTTPMethod.PUT, com.example.data.api.HTTPMethod.PATCH -> NeonAmber
                                com.example.data.api.HTTPMethod.DELETE -> NeonRose
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = methodColor) {
                                Text(
                                    ep.method.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(ep.path, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Text(ep.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MigrationsTab(schema: DatabaseSchema) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(schema.migrations) { mig ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Migration v${mig.version}: ${mig.description}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (mig.status == MigrationStatus.APPLIED) NeonEmerald else NeonAmber
                        ) {
                            Text(
                                mig.status.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    mig.operations.forEach { op ->
                        Text("  • $op", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvironmentTab(envConfig: EnvironmentConfig) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Environment Configuration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Divider()
            EnvItem("DATABASE_URL", envConfig.databaseUrl)
            EnvItem("API_URL", envConfig.apiUrl)
            EnvItem("AUTH_DOMAIN", envConfig.authDomain)
            EnvItem("DEVELOPMENT_MODE", envConfig.isDevelopmentMode.toString())
            EnvItem("PROVIDER_TYPE", envConfig.providerType)
        }
    }
}

@Composable
private fun EnvItem(key: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ElectricIndigo)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

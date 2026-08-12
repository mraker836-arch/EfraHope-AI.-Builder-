package com.example.data.ai.agents

import com.example.data.ai.service.AIService
import com.example.data.api.APIContractGenerator
import com.example.data.api.APIRouteContract
import com.example.data.db.schema.*
import java.util.UUID

class DatabasePlannerAgent(
    private val aiService: AIService
) {
    private val contractGenerator = APIContractGenerator()
    private val schemaValidator = SchemaValidator()

    fun planDatabase(prompt: String, projectName: String): Pair<DatabaseSchema, APIRouteContract> {
        val lowerPrompt = prompt.lowercase()

        val entities = mutableListOf<SchemaEntity>()
        val relationships = mutableListOf<SchemaRelationship>()
        val indexes = mutableListOf<SchemaIndex>()
        val crudSpecs = mutableListOf<CrudOperationSpec>()

        if (lowerPrompt.contains("store") || lowerPrompt.contains("shop") || lowerPrompt.contains("e-commerce") || lowerPrompt.contains("product")) {
            // E-Commerce / Store Schema
            entities.add(
                SchemaEntity(
                    id = "ent-product",
                    name = "Product",
                    description = "Store items available for sale",
                    fields = listOf(
                        SchemaField("f-p1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f-p2", "title", DataType.STRING, nullable = false),
                        SchemaField("f-p3", "sku", DataType.STRING, unique = true),
                        SchemaField("f-p4", "price", DataType.DECIMAL, nullable = false, defaultValue = "0.00"),
                        SchemaField("f-p5", "stockQuantity", DataType.INTEGER, defaultValue = "0"),
                        SchemaField("f-p6", "supplierId", DataType.UUID, nullable = true)
                    ),
                    primaryKey = "id"
                )
            )

            entities.add(
                SchemaEntity(
                    id = "ent-customer",
                    name = "Customer",
                    description = "Registered customers and buyers",
                    fields = listOf(
                        SchemaField("f-c1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f-c2", "fullName", DataType.STRING, nullable = false),
                        SchemaField("f-c3", "email", DataType.STRING, unique = true, nullable = false),
                        SchemaField("f-c4", "phone", DataType.STRING, nullable = true),
                        SchemaField("f-c5", "address", DataType.TEXT, nullable = true)
                    ),
                    primaryKey = "id"
                )
            )

            entities.add(
                SchemaEntity(
                    id = "ent-supplier",
                    name = "Supplier",
                    description = "Product inventory suppliers",
                    fields = listOf(
                        SchemaField("f-s1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f-s2", "companyName", DataType.STRING, nullable = false),
                        SchemaField("f-s3", "contactEmail", DataType.STRING),
                        SchemaField("f-s4", "country", DataType.STRING)
                    ),
                    primaryKey = "id"
                )
            )

            entities.add(
                SchemaEntity(
                    id = "ent-sale",
                    name = "Sale",
                    description = "Completed sales transactions",
                    fields = listOf(
                        SchemaField("f-sl1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f-sl2", "customerId", DataType.UUID, nullable = false),
                        SchemaField("f-sl3", "totalAmount", DataType.DECIMAL, nullable = false),
                        SchemaField("f-sl4", "status", DataType.ENUM, defaultValue = "COMPLETED")
                    ),
                    primaryKey = "id"
                )
            )

            entities.add(
                SchemaEntity(
                    id = "ent-saleitem",
                    name = "SaleItem",
                    description = "Individual line items in a sale",
                    fields = listOf(
                        SchemaField("f-si1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f-si2", "saleId", DataType.UUID, nullable = false),
                        SchemaField("f-si3", "productId", DataType.UUID, nullable = false),
                        SchemaField("f-si4", "quantity", DataType.INTEGER, defaultValue = "1"),
                        SchemaField("f-si5", "unitPrice", DataType.DECIMAL)
                    ),
                    primaryKey = "id"
                )
            )

            relationships.add(
                SchemaRelationship("rel-1", "Product", "Supplier", RelationshipType.MANY_TO_ONE, "supplierId", "Product belongs to supplier")
            )
            relationships.add(
                SchemaRelationship("rel-2", "Sale", "Customer", RelationshipType.MANY_TO_ONE, "customerId", "Sale made by customer")
            )
            relationships.add(
                SchemaRelationship("rel-3", "SaleItem", "Sale", RelationshipType.MANY_TO_ONE, "saleId", "Line item belongs to sale")
            )
            relationships.add(
                SchemaRelationship("rel-4", "SaleItem", "Product", RelationshipType.MANY_TO_ONE, "productId", "Line item refers to product")
            )

            indexes.add(SchemaIndex("idx-prod-sku", "idx_product_sku", "Product", listOf("sku"), isUnique = true))
            indexes.add(SchemaIndex("idx-cust-email", "idx_customer_email", "Customer", listOf("email"), isUnique = true))
        } else if (lowerPrompt.contains("rice") || lowerPrompt.contains("trading") || lowerPrompt.contains("inventory")) {
            // Rice Trading / Inventory Schema
            entities.add(
                SchemaEntity(
                    id = "ent-inventory",
                    name = "RiceBatch",
                    description = "Rice grain inventory batches",
                    fields = listOf(
                        SchemaField("f-rb1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f-rb2", "variety", DataType.STRING, nullable = false),
                        SchemaField("f-rb3", "originCountry", DataType.STRING),
                        SchemaField("f-rb4", "quantityTons", DataType.DECIMAL, defaultValue = "0"),
                        SchemaField("f-rb5", "pricePerTon", DataType.DECIMAL)
                    ),
                    primaryKey = "id"
                )
            )

            entities.add(
                SchemaEntity(
                    id = "ent-trade-order",
                    name = "TradeOrder",
                    description = "Bulk import/export trade orders",
                    fields = listOf(
                        SchemaField("f-to1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f-to2", "clientName", DataType.STRING, nullable = false),
                        SchemaField("f-to3", "batchId", DataType.UUID, nullable = false),
                        SchemaField("f-to4", "orderTons", DataType.DECIMAL),
                        SchemaField("f-to5", "status", DataType.STRING, defaultValue = "PENDING")
                    ),
                    primaryKey = "id"
                )
            )

            relationships.add(
                SchemaRelationship("rel-tr1", "TradeOrder", "RiceBatch", RelationshipType.MANY_TO_ONE, "batchId", "Order references rice batch")
            )
        } else {
            // General Default Task / Workspace Schema
            entities.add(
                SchemaEntity(
                    id = "ent-task",
                    name = "TaskItem",
                    description = "User task items",
                    fields = listOf(
                        SchemaField("f-t1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f-t2", "title", DataType.STRING, nullable = false),
                        SchemaField("f-t3", "isCompleted", DataType.BOOLEAN, defaultValue = "false"),
                        SchemaField("f-t4", "priority", DataType.STRING, defaultValue = "MEDIUM")
                    ),
                    primaryKey = "id"
                )
            )
        }

        // Generate default CRUD specifications for each entity
        entities.forEach { entity ->
            crudSpecs.add(
                CrudOperationSpec(
                    entityName = entity.name,
                    searchFields = entity.fields.filter { it.type == DataType.STRING }.map { it.name }
                )
            )
        }

        val initialMigration = SchemaMigration(
            id = "mig-1",
            version = 1,
            description = "Initial schema creation for $projectName",
            operations = entities.map { "CREATE_TABLE ${it.name}" },
            status = MigrationStatus.APPLIED
        )

        val schema = DatabaseSchema(
            id = "schema-${UUID.randomUUID().toString().take(8)}",
            projectName = projectName,
            entities = entities,
            relationships = relationships,
            indexes = indexes,
            migrations = listOf(initialMigration),
            crudRequirements = crudSpecs
        )

        val routeContract = contractGenerator.generateFromSchema(schema)
        return Pair(schema, routeContract)
    }

    fun planSchemaChange(
        existingSchema: DatabaseSchema,
        changePrompt: String
    ): SchemaChangePlan {
        val lower = changePrompt.lowercase()
        val changes = mutableListOf<SchemaChangeItem>()
        var isDestructive = false

        if (lower.contains("loyalty") || lower.contains("points")) {
            val customerEntity = existingSchema.entities.find { it.name.equals("Customer", ignoreCase = true) }
            if (customerEntity != null && !customerEntity.fields.any { it.name.equals("loyaltyPoints", ignoreCase = true) }) {
                changes.add(
                    SchemaChangeItem(
                        id = "chg-${UUID.randomUUID().toString().take(6)}",
                        type = SchemaChangeType.ADD_FIELD,
                        entityName = "Customer",
                        targetName = "loyaltyPoints",
                        description = "Add loyaltyPoints INTEGER field (default 0) to Customer entity",
                        riskLevel = ChangeRiskLevel.LOW
                    )
                )
            }
        }

        if (lower.contains("remove") || lower.contains("delete") || lower.contains("drop")) {
            isDestructive = true
            changes.add(
                SchemaChangeItem(
                    id = "chg-${UUID.randomUUID().toString().take(6)}",
                    type = SchemaChangeType.REMOVE_FIELD,
                    entityName = "TargetEntity",
                    targetName = "deprecatedField",
                    description = "Remove deprecated field (DESTRUCTIVE: existing column data will be removed)",
                    riskLevel = ChangeRiskLevel.DESTRUCTIVE
                )
            )
        }

        if (changes.isEmpty()) {
            // General field addition
            changes.add(
                SchemaChangeItem(
                    id = "chg-${UUID.randomUUID().toString().take(6)}",
                    type = SchemaChangeType.ADD_FIELD,
                    entityName = existingSchema.entities.firstOrNull()?.name ?: "AppEntity",
                    targetName = "customNote",
                    description = "Add customNote TEXT field to entity",
                    riskLevel = ChangeRiskLevel.LOW
                )
            )
        }

        return SchemaChangePlan(
            planId = "plan-${UUID.randomUUID().toString().take(8)}",
            projectId = existingSchema.projectName,
            description = "Schema modification for: $changePrompt",
            changes = changes,
            requiresExplicitApproval = isDestructive || changes.any { it.riskLevel == ChangeRiskLevel.HIGH || it.riskLevel == ChangeRiskLevel.DESTRUCTIVE }
        )
    }

    fun applyChangePlan(
        existingSchema: DatabaseSchema,
        plan: SchemaChangePlan
    ): DatabaseSchema {
        val updatedEntities = existingSchema.entities.toMutableList()

        for (change in plan.changes) {
            when (change.type) {
                SchemaChangeType.ADD_FIELD -> {
                    val idx = updatedEntities.indexOfFirst { it.name.equals(change.entityName, ignoreCase = true) }
                    if (idx != -1) {
                        val entity = updatedEntities[idx]
                        if (!entity.fields.any { it.name.equals(change.targetName, ignoreCase = true) }) {
                            val newField = SchemaField(
                                id = "f-${UUID.randomUUID().toString().take(6)}",
                                name = change.targetName,
                                type = DataType.INTEGER,
                                defaultValue = "0",
                                description = change.description
                            )
                            updatedEntities[idx] = entity.copy(fields = entity.fields + newField)
                        }
                    }
                }
                SchemaChangeType.REMOVE_FIELD -> {
                    val idx = updatedEntities.indexOfFirst { it.name.equals(change.entityName, ignoreCase = true) }
                    if (idx != -1) {
                        val entity = updatedEntities[idx]
                        updatedEntities[idx] = entity.copy(fields = entity.fields.filterNot { it.name.equals(change.targetName, ignoreCase = true) })
                    }
                }
                else -> { /* Other change types supported in schema migrations */ }
            }
        }

        val newMigration = SchemaMigration(
            id = "mig-${existingSchema.migrations.size + 1}",
            version = existingSchema.version + 1,
            description = plan.description,
            operations = plan.changes.map { "${it.type} ON ${it.entityName}.${it.targetName}" },
            status = MigrationStatus.APPLIED
        )

        return existingSchema.copy(
            entities = updatedEntities,
            migrations = existingSchema.migrations + newMigration,
            version = existingSchema.version + 1
        )
    }
}

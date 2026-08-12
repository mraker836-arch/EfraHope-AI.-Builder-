package com.example.data.db.schema

import com.example.data.models.AppError
import com.example.data.models.ErrorSeverity
import com.example.data.models.ErrorType

data class SchemaValidationResult(
    val isValid: Boolean,
    val errors: List<AppError>,
    val warnings: List<AppError>
)

class SchemaValidator {

    fun validate(schema: DatabaseSchema): SchemaValidationResult {
        val errors = mutableListOf<AppError>()
        val warnings = mutableListOf<AppError>()

        val entityMap = mutableMapOf<String, SchemaEntity>()
        val entityNamesLower = mutableSetOf<String>()

        // 1. Validate Entities
        for (entity in schema.entities) {
            val lowerName = entity.name.lowercase().trim()
            if (lowerName.isEmpty()) {
                errors.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.ERROR,
                        message = "Entity ID '${entity.id}' has an empty or blank name.",
                        source = "SchemaValidator"
                    )
                )
                continue
            }

            if (entityNamesLower.contains(lowerName)) {
                errors.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.ERROR,
                        message = "Duplicate entity name found: '${entity.name}'. Entity names must be unique.",
                        source = "SchemaValidator"
                    )
                )
            } else {
                entityNamesLower.add(lowerName)
                entityMap[entity.name] = entity
            }

            // Validate Fields in Entity
            validateEntityFields(entity, errors, warnings)
        }

        // 2. Validate Relationships
        val relationshipIds = mutableSetOf<String>()
        for (rel in schema.relationships) {
            if (relationshipIds.contains(rel.id)) {
                errors.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.ERROR,
                        message = "Duplicate relationship ID found: '${rel.id}'.",
                        source = "SchemaValidator"
                    )
                )
            } else {
                relationshipIds.add(rel.id)
            }

            val fromEntity = entityMap[rel.fromEntity]
            val toEntity = entityMap[rel.toEntity]

            if (fromEntity == null) {
                errors.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.ERROR,
                        message = "Relationship '${rel.id}' references non-existent source entity '${rel.fromEntity}'.",
                        source = "SchemaValidator"
                    )
                )
            }

            if (toEntity == null) {
                errors.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.ERROR,
                        message = "Relationship '${rel.id}' references non-existent target entity '${rel.toEntity}'.",
                        source = "SchemaValidator"
                    )
                )
            }

            if (fromEntity != null && rel.foreignKey.isNotEmpty()) {
                val fkFieldExists = fromEntity.fields.any { it.name.equals(rel.foreignKey, ignoreCase = true) }
                if (!fkFieldExists) {
                    errors.add(
                        AppError(
                            type = ErrorType.SCHEMA,
                            severity = ErrorSeverity.ERROR,
                            message = "Foreign key field '${rel.foreignKey}' in relationship '${rel.id}' does not exist on source entity '${fromEntity.name}'.",
                            source = "SchemaValidator"
                        )
                    )
                }
            }
        }

        // 3. Validate Indexes
        val indexNames = mutableSetOf<String>()
        for (idx in schema.indexes) {
            if (indexNames.contains(idx.name)) {
                warnings.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.WARNING,
                        message = "Duplicate index name '${idx.name}'.",
                        source = "SchemaValidator"
                    )
                )
            } else {
                indexNames.add(idx.name)
            }

            val tableEntity = entityMap[idx.tableName]
            if (tableEntity == null) {
                errors.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.ERROR,
                        message = "Index '${idx.name}' references non-existent entity '${idx.tableName}'.",
                        source = "SchemaValidator"
                    )
                )
            } else {
                for (field in idx.fields) {
                    if (!tableEntity.fields.any { it.name.equals(field, ignoreCase = true) }) {
                        errors.add(
                            AppError(
                                type = ErrorType.SCHEMA,
                                severity = ErrorSeverity.ERROR,
                                message = "Index '${idx.name}' references non-existent field '$field' on entity '${tableEntity.name}'.",
                                source = "SchemaValidator"
                            )
                        )
                    }
                }
            }
        }

        // 4. Circular Foreign Key Check
        checkCircularDependencies(schema, errors)

        val hasErrors = errors.isNotEmpty()
        return SchemaValidationResult(
            isValid = !hasErrors,
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateEntityFields(
        entity: SchemaEntity,
        errors: MutableList<AppError>,
        warnings: MutableList<AppError>
    ) {
        if (entity.fields.isEmpty()) {
            errors.add(
                AppError(
                    type = ErrorType.SCHEMA,
                    severity = ErrorSeverity.ERROR,
                    message = "Entity '${entity.name}' has no fields defined.",
                    source = "SchemaValidator"
                )
            )
            return
        }

        val fieldNames = mutableSetOf<String>()
        var hasPk = false

        for (field in entity.fields) {
            val lowerFieldName = field.name.lowercase().trim()
            if (lowerFieldName.isEmpty()) {
                errors.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.ERROR,
                        message = "Entity '${entity.name}' contains a field with an empty name.",
                        source = "SchemaValidator"
                    )
                )
                continue
            }

            if (fieldNames.contains(lowerFieldName)) {
                errors.add(
                    AppError(
                        type = ErrorType.SCHEMA,
                        severity = ErrorSeverity.ERROR,
                        message = "Duplicate field '${field.name}' in entity '${entity.name}'.",
                        source = "SchemaValidator"
                    )
                )
            } else {
                fieldNames.add(lowerFieldName)
            }

            if (field.isPrimaryKey || field.name.equals(entity.primaryKey, ignoreCase = true)) {
                hasPk = true
            }
        }

        if (!hasPk) {
            errors.add(
                AppError(
                    type = ErrorType.SCHEMA,
                    severity = ErrorSeverity.ERROR,
                    message = "Entity '${entity.name}' is missing a primary key field matching '${entity.primaryKey}'.",
                    source = "SchemaValidator"
                )
            )
        }
    }

    private fun checkCircularDependencies(schema: DatabaseSchema, errors: MutableList<AppError>) {
        val graph = mutableMapOf<String, MutableSet<String>>()
        for (rel in schema.relationships) {
            val neighbors = graph.getOrPut(rel.fromEntity) { mutableSetOf() }
            neighbors.add(rel.toEntity)
        }

        val visited = mutableSetOf<String>()
        val recStack = mutableSetOf<String>()

        fun dfs(node: String, path: List<String>) {
            visited.add(node)
            recStack.add(node)

            for (neighbor in graph[node] ?: emptySet()) {
                if (!visited.contains(neighbor)) {
                    dfs(neighbor, path + neighbor)
                } else if (recStack.contains(neighbor)) {
                    errors.add(
                        AppError(
                            type = ErrorType.SCHEMA,
                            severity = ErrorSeverity.WARNING,
                            message = "Circular relationship detected in schema: ${(path + neighbor).joinToString(" -> ")}",
                            source = "SchemaValidator"
                        )
                    )
                }
            }

            recStack.remove(node)
        }

        for (entity in schema.entities) {
            if (!visited.contains(entity.name)) {
                dfs(entity.name, listOf(entity.name))
            }
        }
    }
}

package com.example.data.db.schema

enum class DataType {
    STRING,
    TEXT,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    DATETIME,
    UUID,
    JSON,
    ENUM
}

enum class RelationshipType {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY
}

enum class MigrationStatus {
    PENDING,
    APPLIED,
    FAILED,
    ROLLED_BACK
}

enum class DatabaseHealthStatus {
    NOT_CONFIGURED,
    DEVELOPMENT,
    CONNECTED,
    WARNING,
    ERROR
}

enum class ChangeRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    DESTRUCTIVE
}

enum class SchemaChangeType {
    CREATE_ENTITY,
    ALTER_ENTITY,
    DROP_ENTITY,
    ADD_FIELD,
    ALTER_FIELD,
    REMOVE_FIELD,
    ADD_RELATIONSHIP,
    REMOVE_RELATIONSHIP,
    ADD_INDEX,
    REMOVE_INDEX
}

data class SchemaField(
    val id: String,
    val name: String,
    val type: DataType,
    val nullable: Boolean = false,
    val unique: Boolean = false,
    val defaultValue: String? = null,
    val description: String = "",
    val isPrimaryKey: Boolean = false
)

data class SchemaRelationship(
    val id: String,
    val fromEntity: String,
    val toEntity: String,
    val type: RelationshipType,
    val foreignKey: String,
    val description: String = ""
)

data class SchemaIndex(
    val id: String,
    val name: String,
    val tableName: String,
    val fields: List<String>,
    val isUnique: Boolean = false
)

data class SchemaEntity(
    val id: String,
    val name: String,
    val description: String = "",
    val fields: List<SchemaField>,
    val primaryKey: String = "id",
    val indexes: List<SchemaIndex> = emptyList(),
    val timestamps: Boolean = true
)

data class CrudOperationSpec(
    val entityName: String,
    val allowCreate: Boolean = true,
    val allowRead: Boolean = true,
    val allowUpdate: Boolean = true,
    val allowDelete: Boolean = true,
    val allowList: Boolean = true,
    val searchFields: List<String> = emptyList()
)

data class SchemaMigration(
    val id: String,
    val version: Int,
    val description: String,
    val operations: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val status: MigrationStatus = MigrationStatus.PENDING
)

data class DatabaseSchema(
    val id: String,
    val projectName: String,
    val entities: List<SchemaEntity> = emptyList(),
    val relationships: List<SchemaRelationship> = emptyList(),
    val indexes: List<SchemaIndex> = emptyList(),
    val migrations: List<SchemaMigration> = emptyList(),
    val crudRequirements: List<CrudOperationSpec> = emptyList(),
    val providerType: String = "DEVELOPMENT_MOCK",
    val version: Int = 1
)

data class SchemaChangeItem(
    val id: String,
    val type: SchemaChangeType,
    val entityName: String,
    val targetName: String,
    val description: String,
    val riskLevel: ChangeRiskLevel
)

data class SchemaChangePlan(
    val planId: String,
    val projectId: String,
    val description: String,
    val changes: List<SchemaChangeItem>,
    val isValid: Boolean = true,
    val requiresExplicitApproval: Boolean = false
)

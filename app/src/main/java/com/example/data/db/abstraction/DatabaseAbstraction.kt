package com.example.data.db.abstraction

import com.example.data.db.schema.DataType
import com.example.data.db.schema.DatabaseSchema
import com.example.data.db.schema.SchemaEntity
import java.util.UUID

interface DatabaseProvider {
    fun connect(): Boolean
    fun disconnect()
    fun isConnected(): Boolean
    fun query(entityName: String, filter: Map<String, Any> = emptyMap()): List<Map<String, Any>>
    fun insert(entityName: String, record: Map<String, Any>): Map<String, Any>
    fun update(entityName: String, id: String, record: Map<String, Any>): Map<String, Any>?
    fun delete(entityName: String, id: String): Boolean
    fun find(entityName: String, id: String): Map<String, Any>?
    fun executeTransaction(block: () -> Unit)
}

class MockDatabaseProvider : DatabaseProvider {
    private var connected = false
    private val tables = mutableMapOf<String, MutableMap<String, MutableMap<String, Any>>>()

    override fun connect(): Boolean {
        connected = true
        return true
    }

    override fun disconnect() {
        connected = false
    }

    override fun isConnected(): Boolean = connected

    override fun query(entityName: String, filter: Map<String, Any>): List<Map<String, Any>> {
        val table = tables[entityName] ?: return emptyList()
        if (filter.isEmpty()) return table.values.toList()

        return table.values.filter { record ->
            filter.all { (key, expectedValue) ->
                val actual = record[key]
                actual?.toString()?.lowercase() == expectedValue.toString().lowercase()
            }
        }
    }

    override fun insert(entityName: String, record: Map<String, Any>): Map<String, Any> {
        val table = tables.getOrPut(entityName) { mutableMapOf() }
        val mutableRecord = record.toMutableMap()

        val id = (mutableRecord["id"] as? String) ?: UUID.randomUUID().toString()
        mutableRecord["id"] = id
        if (!mutableRecord.containsKey("createdAt")) {
            mutableRecord["createdAt"] = System.currentTimeMillis()
        }
        mutableRecord["updatedAt"] = System.currentTimeMillis()

        table[id] = mutableRecord
        return mutableRecord
    }

    override fun update(entityName: String, id: String, record: Map<String, Any>): Map<String, Any>? {
        val table = tables[entityName] ?: return null
        val existing = table[id] ?: return null

        val updated = existing.toMutableMap()
        updated.putAll(record)
        updated["updatedAt"] = System.currentTimeMillis()

        table[id] = updated
        return updated
    }

    override fun delete(entityName: String, id: String): Boolean {
        val table = tables[entityName] ?: return false
        return table.remove(id) != null
    }

    override fun find(entityName: String, id: String): Map<String, Any>? {
        return tables[entityName]?.get(id)
    }

    override fun executeTransaction(block: () -> Unit) {
        block()
    }

    fun seedFromSchema(schema: DatabaseSchema) {
        connect()
        for (entity in schema.entities) {
            val table = tables.getOrPut(entity.name) { mutableMapOf() }
            if (table.isEmpty()) {
                // Generate 3 sample mock records for preview
                repeat(3) { index ->
                    val mockRecord = mutableMapOf<String, Any>()
                    val recordId = "${entity.name.lowercase()}-${index + 1}"
                    mockRecord["id"] = recordId

                    for (field in entity.fields) {
                        if (field.name.equals("id", ignoreCase = true)) continue
                        mockRecord[field.name] = generateMockFieldValue(field.name, field.type, index + 1)
                    }

                    table[recordId] = mockRecord
                }
            }
        }
    }

    private fun generateMockFieldValue(fieldName: String, type: DataType, index: Int): Any {
        val lower = fieldName.lowercase()
        return when {
            lower.contains("name") || lower.contains("title") -> "Sample $fieldName #$index"
            lower.contains("email") -> "user$index@example.com"
            lower.contains("phone") -> "+1-555-010$index"
            lower.contains("price") || lower.contains("amount") -> 19.99 * index.toDouble()
            lower.contains("status") -> if (index % 2 == 0) "ACTIVE" else "PENDING"
            lower.contains("code") -> "SKU-100$index"
            else -> when (type) {
                DataType.STRING, DataType.TEXT -> "Sample $fieldName text $index"
                DataType.INTEGER -> 10 * index
                DataType.DECIMAL -> 9.99 * index.toDouble()
                DataType.BOOLEAN -> index % 2 == 1
                DataType.DATE, DataType.DATETIME -> "2026-08-1${index}T10:00:00Z"
                DataType.UUID -> UUID.randomUUID().toString()
                DataType.JSON -> "{\"meta\": \"sample_$index\"}"
                DataType.ENUM -> "OPTION_$index"
            }
        }
    }
}

class GenericRepository(
    private val entityName: String,
    private val dbProvider: DatabaseProvider
) {
    fun findAll(): List<Map<String, Any>> = dbProvider.query(entityName)

    fun findById(id: String): Map<String, Any>? = dbProvider.find(entityName, id)

    fun search(key: String, value: Any): List<Map<String, Any>> {
        return dbProvider.query(entityName, mapOf(key to value))
    }

    fun create(record: Map<String, Any>): Map<String, Any> {
        return dbProvider.insert(entityName, record)
    }

    fun update(id: String, record: Map<String, Any>): Map<String, Any>? {
        return dbProvider.update(entityName, id, record)
    }

    fun delete(id: String): Boolean {
        return dbProvider.delete(entityName, id)
    }
}

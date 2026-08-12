package com.example

import com.example.data.ai.agents.DatabasePlannerAgent
import com.example.data.ai.service.AIService
import com.example.data.api.APIContractGenerator
import com.example.data.api.EnvironmentConfig
import com.example.data.api.HTTPMethod
import com.example.data.db.abstraction.GenericRepository
import com.example.data.db.abstraction.MockDatabaseProvider
import com.example.data.db.schema.*
import com.example.data.models.ErrorSeverity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AIEnginePhase6Test {

    private lateinit var plannerAgent: DatabasePlannerAgent
    private lateinit var validator: SchemaValidator
    private lateinit var mockDbProvider: MockDatabaseProvider

    @Before
    fun setUp() {
        plannerAgent = DatabasePlannerAgent(AIService())
        validator = SchemaValidator()
        mockDbProvider = MockDatabaseProvider()
    }

    @Test
    fun testStoreDatabasePlanning() {
        val (schema, apiContract) = plannerAgent.planDatabase(
            prompt = "Build a store management application with products, customers, suppliers and sales",
            projectName = "E-Commerce Pro"
        )

        assertNotNull(schema)
        assertEquals("E-Commerce Pro", schema.projectName)
        assertTrue(schema.entities.any { it.name == "Product" })
        assertTrue(schema.entities.any { it.name == "Customer" })
        assertTrue(schema.entities.any { it.name == "Supplier" })
        assertTrue(schema.entities.any { it.name == "Sale" })
        assertTrue(schema.entities.any { it.name == "SaleItem" })

        val validationResult = validator.validate(schema)
        assertTrue("Generated schema must be valid", validationResult.isValid)
        assertTrue("No validation errors expected", validationResult.errors.isEmpty())

        assertNotNull(apiContract)
        assertTrue(apiContract.endpoints.isNotEmpty())
        assertTrue(apiContract.endpoints.any { it.entityName == "Product" && it.method == HTTPMethod.GET })
    }

    @Test
    fun testSchemaValidatorDetectsDuplicatesAndMissingPK() {
        val invalidSchema = DatabaseSchema(
            id = "invalid-s1",
            projectName = "Test App",
            entities = listOf(
                SchemaEntity(
                    id = "e1",
                    name = "User",
                    fields = listOf(
                        SchemaField("f1", "id", DataType.UUID, isPrimaryKey = true),
                        SchemaField("f2", "id", DataType.STRING) // Duplicate field name 'id'
                    )
                ),
                SchemaEntity(
                    id = "e2",
                    name = "User", // Duplicate entity name 'User'
                    fields = listOf(
                        SchemaField("f3", "name", DataType.STRING) // Missing PK
                    )
                )
            )
        )

        val result = validator.validate(invalidSchema)
        assertFalse("Schema with duplicate entity/fields must fail validation", result.isValid)
        assertTrue(result.errors.any { it.message.contains("Duplicate entity name") })
        assertTrue(result.errors.any { it.message.contains("Duplicate field") })
    }

    @Test
    fun testSchemaValidatorDetectsInvalidRelationships() {
        val schemaWithBadRel = DatabaseSchema(
            id = "invalid-rel-1",
            projectName = "Rel Test App",
            entities = listOf(
                SchemaEntity(
                    id = "e1",
                    name = "Order",
                    fields = listOf(SchemaField("f1", "id", DataType.UUID, isPrimaryKey = true))
                )
            ),
            relationships = listOf(
                SchemaRelationship(
                    id = "r1",
                    fromEntity = "Order",
                    toEntity = "NonExistentUser", // Invalid target entity
                    type = RelationshipType.MANY_TO_ONE,
                    foreignKey = "userId"
                )
            )
        )

        val result = validator.validate(schemaWithBadRel)
        assertFalse("Schema with non-existent relationship target must fail validation", result.isValid)
        assertTrue(result.errors.any { it.message.contains("non-existent target entity") })
    }

    @Test
    fun testSchemaChangePlanAndApprovalFlow() {
        val (initialSchema, _) = plannerAgent.planDatabase(
            prompt = "Build a store management app with products and customers",
            projectName = "Store App"
        )

        val changePlan = plannerAgent.planSchemaChange(
            existingSchema = initialSchema,
            changePrompt = "Add customer loyalty points."
        )

        assertNotNull(changePlan)
        assertFalse(changePlan.changes.isEmpty())
        val addChange = changePlan.changes.find { it.targetName == "loyaltyPoints" }
        assertNotNull(addChange)
        assertEquals(SchemaChangeType.ADD_FIELD, addChange?.type)

        val updatedSchema = plannerAgent.applyChangePlan(initialSchema, changePlan)
        val customerEntity = updatedSchema.entities.find { it.name == "Customer" }
        assertNotNull(customerEntity)
        assertTrue(customerEntity!!.fields.any { it.name == "loyaltyPoints" })
    }

    @Test
    fun testMockDatabaseProviderAndRepository() {
        val (schema, _) = plannerAgent.planDatabase("store app", "Test App")
        mockDbProvider.seedFromSchema(schema)

        val productRepo = GenericRepository("Product", mockDbProvider)
        val products = productRepo.findAll()

        assertFalse("Mock provider should seed initial records", products.isEmpty())

        val newProduct = mapOf(
            "title" to "Wireless Headphones",
            "price" to 149.99,
            "sku" to "HEADPHONE-001"
        )
        val created = productRepo.create(newProduct)
        assertNotNull(created["id"])

        val createdId = created["id"].toString()
        val fetched = productRepo.findById(createdId)
        assertNotNull(fetched)
        assertEquals("Wireless Headphones", fetched!!["title"])

        val updated = productRepo.update(createdId, mapOf("price" to 129.99))
        assertNotNull(updated)
        assertEquals(129.99, updated!!["price"])

        val deleted = productRepo.delete(createdId)
        assertTrue(deleted)
        assertNull(productRepo.findById(createdId))
    }

    @Test
    fun testEnvironmentConfig() {
        val env = EnvironmentConfig()
        assertTrue(env.isDevelopmentMode)
        assertEquals("DEVELOPMENT_MOCK", env.providerType)
        assertNotNull(env.databaseUrl)
        assertNotNull(env.apiUrl)
    }
}

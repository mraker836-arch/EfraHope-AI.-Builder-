package com.example.data.api

import com.example.data.db.schema.DatabaseSchema

enum class HTTPMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE
}

data class APIEndpoint(
    val id: String,
    val method: HTTPMethod,
    val path: String,
    val description: String,
    val entityName: String,
    val requestSchema: String = "{}",
    val responseSchema: String = "{}",
    val requiresAuth: Boolean = false,
    val errors: List<String> = listOf("400 Bad Request", "401 Unauthorized", "404 Not Found", "500 Internal Error")
)

data class APIRouteContract(
    val id: String,
    val projectName: String,
    val version: String = "v1",
    val baseUrl: String = "/api/v1",
    val endpoints: List<APIEndpoint> = emptyList()
)

data class EnvironmentConfig(
    val databaseUrl: String = "mock://localhost/devdb",
    val apiUrl: String = "http://localhost:8080/api/v1",
    val authDomain: String = "dev-auth.efrahope.ai",
    val isDevelopmentMode: Boolean = true,
    val providerType: String = "DEVELOPMENT_MOCK"
)

class APIContractGenerator {
    fun generateFromSchema(schema: DatabaseSchema): APIRouteContract {
        val endpoints = mutableListOf<APIEndpoint>()

        for (entity in schema.entities) {
            val entitySlug = entity.name.lowercase().pluralize()
            val basePath = "/api/v1/$entitySlug"

            // GET List
            endpoints.add(
                APIEndpoint(
                    id = "ep-${entity.name.lowercase()}-list",
                    method = HTTPMethod.GET,
                    path = basePath,
                    description = "Fetch list of ${entity.name} records",
                    entityName = entity.name,
                    responseSchema = "Array<${entity.name}>"
                )
            )

            // GET Item
            endpoints.add(
                APIEndpoint(
                    id = "ep-${entity.name.lowercase()}-get",
                    method = HTTPMethod.GET,
                    path = "$basePath/{id}",
                    description = "Fetch single ${entity.name} by ID",
                    entityName = entity.name,
                    responseSchema = entity.name
                )
            )

            // POST Create
            endpoints.add(
                APIEndpoint(
                    id = "ep-${entity.name.lowercase()}-create",
                    method = HTTPMethod.POST,
                    path = basePath,
                    description = "Create new ${entity.name} record",
                    entityName = entity.name,
                    requestSchema = "Create${entity.name}Input",
                    responseSchema = entity.name,
                    requiresAuth = true
                )
            )

            // PUT Update
            endpoints.add(
                APIEndpoint(
                    id = "ep-${entity.name.lowercase()}-update",
                    method = HTTPMethod.PUT,
                    path = "$basePath/{id}",
                    description = "Update existing ${entity.name} record",
                    entityName = entity.name,
                    requestSchema = "Update${entity.name}Input",
                    responseSchema = entity.name,
                    requiresAuth = true
                )
            )

            // DELETE
            endpoints.add(
                APIEndpoint(
                    id = "ep-${entity.name.lowercase()}-delete",
                    method = HTTPMethod.DELETE,
                    path = "$basePath/{id}",
                    description = "Delete ${entity.name} record by ID",
                    entityName = entity.name,
                    responseSchema = "{ success: Boolean }",
                    requiresAuth = true
                )
            )
        }

        return APIRouteContract(
            id = "contract-${schema.id}",
            projectName = schema.projectName,
            endpoints = endpoints
        )
    }

    private fun String.pluralize(): String {
        return when {
            endsWith("y") -> substring(0, length - 1) + "ies"
            endsWith("s") -> this + "es"
            else -> this + "s"
        }
    }
}

package com.kroegerama.kgen.spec

import com.kroegerama.kgen.OptionSet
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import java.util.*

class SpecParser(
    private val specFile: String,
    private val options: OptionSet
) {
    fun parseAndResolve(): OpenAPI {
        val result = OpenAPIParser().readLocation(specFile, emptyList(), SpecConfig.parseOptions)
        if (!options.allowParseErrors && (!result.messages.isNullOrEmpty() || result.openAPI == null)) {
            result.messages?.forEach { message ->
                System.err.println(message)
            }
            throw IllegalStateException("Cannot parse spec $specFile")
        }

        result.openAPI.fixAnyOfNullRef()
        result.openAPI.flattenPaths()
        result.openAPI.filterOperations()
        result.openAPI.filterSchemas()
        result.openAPI.components?.schemas?.forEach { (name, schema) ->
            schema.name = name
        }
        return result.openAPI
    }

    private fun OpenAPI.flattenPaths() {
        paths?.forEach { (path, pathItem) ->
            val names = path.trim('/').split('/').joinToString(".") { part ->
                part.trim('{', '}')
            }
            flattenPathItem(
                baseName = names,
                pathItem = pathItem
            )
        }
    }

    private fun OpenAPI.flattenPathItem(baseName: String, pathItem: PathItem) {
        pathItem.parameters?.forEach { parameter ->
            flattenParameter(
                baseName = baseName,
                parameter = parameter
            )
        }
        pathItem.readOperationsMap()?.forEach { (method, operation) ->
            val name = operation.operationId ?: operation.run { "$method.$baseName" }
            operation.parameters?.forEach { parameter ->
                flattenParameter(name, parameter)
            }
            operation.requestBody?.content?.forEach { (mimeType, mediaType) ->
                flattenMediaType("$baseName.request", mediaType)
            }
            operation.responses?.forEach { (code, apiResponse) ->
                apiResponse.content?.forEach { (mimeType, mediaType) ->
                    flattenMediaType("$baseName.$code.response", mediaType)
                }
            }
        }
    }

    private fun OpenAPI.flattenMediaType(baseName: String, mediaType: MediaType) {
        mediaType.schema?.let { schema ->
            val type = schema.getSpecType()
            if (type.needsName) {
                val name = createSchemaName(baseName)
                schema(name, schema)
                mediaType.schema = Schema<Any>().`$ref`("#/components/schemas/$name")
            }
        }
    }

    private fun OpenAPI.flattenParameter(baseName: String, parameter: Parameter) {
        parameter.schema?.let { schema ->
            val type = schema.getSpecType()
            if (type.needsName) {
                val name = createSchemaName(baseName + "." + parameter.name)
                schema(name, schema)
                parameter.schema = Schema<Any>().`$ref`("#/components/schemas/$name")
            }
        }
    }

    private fun createSchemaName(baseName: String) = baseName.split('.').joinToString("") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun OpenAPI.filterOperations() {
        if (options.limitApis.isEmpty()) {
            return
        }
        val filteredPathsMap = paths?.filterValues { pathItem ->
            fun filterOperation(operation: Operation, clear: () -> Unit) {
                val tags = operation.resolveTags()
                if (tags.none { it in options.limitApis }) {
                    clear()
                }
            }
            pathItem.get?.let { operation -> filterOperation(operation) { pathItem.get = null } }
            pathItem.put?.let { operation -> filterOperation(operation) { pathItem.put = null } }
            pathItem.post?.let { operation -> filterOperation(operation) { pathItem.post = null } }
            pathItem.delete?.let { operation -> filterOperation(operation) { pathItem.delete = null } }
            pathItem.options?.let { operation -> filterOperation(operation) { pathItem.options = null } }
            pathItem.head?.let { operation -> filterOperation(operation) { pathItem.head = null } }
            pathItem.patch?.let { operation -> filterOperation(operation) { pathItem.patch = null } }
            pathItem.trace?.let { operation -> filterOperation(operation) { pathItem.trace = null } }

            pathItem.readOperations().isNotEmpty()
        }.orEmpty()
        paths = Paths().apply { putAll(filteredPathsMap) }
    }

    private fun OpenAPI.fixAnyOfNullRef() {
        val visitor = SpecVisitor(
            openAPI = this,
            options = options
        )

        data class Fix(
            val schema: Schema<*>,
            val ref: String
        )

        val fixes = mutableSetOf<Fix>()
        visitor.visit { schema ->
            val anyOf = schema.anyOf
            if (anyOf != null && anyOf.size == 2) {
                val ref = anyOf.firstNotNullOfOrNull {
                    it.`$ref`
                }
                val nl = anyOf.firstNotNullOfOrNull {
                    it.types.orEmpty().contains("null")
                }
                if (ref != null && nl != null) {
                    fixes += Fix(
                        schema = schema,
                        ref = ref
                    )
                }
            }
        }
        fixes.forEach {
            it.schema.anyOf = null
            it.schema.`$ref` = it.ref
            it.schema.nullable = true
        }
    }

    private fun OpenAPI.filterSchemas() {
        if (options.generateAllNamedSchemas) {
            return
        }
        val visitor = SpecVisitor(
            openAPI = this,
            options = options
        )

        val visited = IdentityHashMap<Any, Any>()
        val marker = Any()
        visitor.visit { schema ->
            visited[schema] = marker
        }
        val removeSchemas = mutableSetOf<String>()
        components?.schemas?.forEach { (name, schema) ->
            if (visited[schema] !== marker) {
                removeSchemas += name
            }
        }
        removeSchemas.forEach { name ->
            if (options.verbose) {
                println("remove unused schema '$name'")
            }
            components?.schemas?.remove(name)
        }
    }
}

package com.kroegerama.kgen.openapi

import com.kroegerama.kgen.cli.OptionSet
import com.kroegerama.kgen.language.asTypeName
import com.kroegerama.kgen.model.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityScheme
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*
import kotlin.collections.HashSet

class OpenAPIAnalyzer : KoinComponent {

    private val openAPI by inject<OpenAPI>()
    private val options by inject<OptionSet>()
    private val allNamedSchemas: Map<String, Schema<*>>

    val namedPrimitives = TreeSet<AnySchemaWithInfo>()
    val namedArrays = TreeSet<ArraySchemaWithInfo>()
    val namedMaps = TreeSet<MapSchemaWithInfo>()
    val enums = TreeSet<AnySchemaWithInfo>()
    val objectTree: ModelTree

    val apis = TreeMap<String, MutableList<OperationWithInfo>>()
    val securitySchemes = TreeMap<String, SecurityScheme>()

    init {
        allNamedSchemas = openAPI.getAllNamedSchemas()

        initApis()
        initSecurity()

        objectTree = buildModelTree()
    }

    fun printModelInfo() {
        if (namedPrimitives.isNotEmpty()) {
            println("### Named Primitives ###")
            namedPrimitives.forEach { println("\t$it") }
            println()
        }

        if (namedArrays.isNotEmpty()) {
            println("### Named Arrays ###")
            namedArrays.forEach { println("\t$it") }
            println()
        }

        if (namedMaps.isNotEmpty()) {
            println("### Named Maps ###")
            namedMaps.forEach { println("\t$it") }
            println()
        }

        if (!objectTree.isEmpty) {
            println("### Object Tree ###")
            if (objectTree.nodes.isNotEmpty()) {
                println("Nodes")
                objectTree.nodes.forEach { println("\t$it") }
                println()
            }
            if (objectTree.unknown.isNotEmpty()) {
                println("Unknown")
                objectTree.unknown.forEach { println("\t$it") }
                println()
            }
        }

        println("### Selected APIs ###")
        apis.forEach { (api, operations) ->
            println(api)
            operations.forEach { println("\t$it") }
        }
        println()
    }

    private fun initSecurity() {
        securitySchemes.putAll(openAPI.components.securitySchemes.orEmpty())
    }

    private fun buildModelTree(): ModelTree {
        val allSchemaInfo = openAPI.getAllSchemas(options.limitApis)
        val rootSchemas = mutableListOf<AnySchemaWithInfo>()
        val unnamedSchemas = mutableListOf<AnySchemaWithInfo>()

        val ignoredAnonymousTypes = listOf(
            SchemaType.Primitive,
            SchemaType.Array,
            SchemaType.Map
        )

        allSchemaInfo.forEach { schemaInfo ->
            val name = schemaInfo.name
            val schemaType = schemaInfo.schemaType

            if (name.isNotEmpty()) {
                when (schemaType) {
                    SchemaType.Primitive -> namedPrimitives.add(schemaInfo)
                    SchemaType.Array -> namedArrays.add(schemaInfo as ArraySchemaWithInfo)
                    SchemaType.Map -> namedMaps.add(schemaInfo as MapSchemaWithInfo)
                    SchemaType.Enum -> enums.add(schemaInfo)
                    else -> rootSchemas.add(schemaInfo)
                }
            } else if (schemaType !in ignoredAnonymousTypes) {
                unnamedSchemas.add(schemaInfo)
            }
        }

        val root = rootSchemas.map { schemaInfo ->
            ModelTreeNode(schemaInfo, HashSet())
        }
        val tmpTree = ModelTree(root, emptyList())

        var foundItem = true
        while (foundItem) {
            foundItem = false
            val accepted = mutableSetOf<AnySchemaWithInfo>()

            unnamedSchemas.forEach { unknown ->
                val schema = unknown.schema
                val type = unknown.schemaType

                //only use Objects and Enums as child classes/enums
                if (type != SchemaType.Object && type != SchemaType.Enum) return@forEach
                val (pName, node) = tmpTree.findNodeWithParameter(schema) ?: return@forEach

                val newNode = ModelTreeNode(
                    schemaInfo = unknown.withName(pName),
                    children = HashSet()
                )
                node.children.add(newNode)
                foundItem = true
                accepted.add(unknown)
            }
            unnamedSchemas.removeAll(accepted)
        }

        val generatedNames = unnamedSchemas.map { unnamed ->
            val name = unnamed.path.joinToString(" ")
            unnamed.withName(name)
        }

        return ModelTree(tmpTree.nodes, generatedNames)
    }

    private fun initApis() {
        val operations = openAPI.getAllOperations(options.limitApis)
        operations.forEach { operationInfo ->
            operationInfo.tags.forEach { tag ->
                val limit = options.limitApis
                if (limit.isEmpty() || limit.contains(tag)) {
                    apis.getOrPut(tag) { mutableListOf() }.add(operationInfo)
                }
            }
        }
    }

    fun findNameFor(schema: Schema<*>): TypeName {
        val knownName = findRawNameFor(schema)
        if (knownName != null) return ClassName(options.modelPackage, knownName.asTypeName())

        val treeName = objectTree.findName(schema)
        if (treeName.isNotEmpty()) {
            return ClassName(options.modelPackage, treeName)
        }

        return when (schema.getSchemaType()) {
            SchemaType.Primitive -> schema.mapToTypeName()
            SchemaType.Object -> schema.mapToTypeName()
            SchemaType.Enum -> schema.mapToTypeName()
            SchemaType.Array -> {
                val arr = schema as ArraySchema
                val inner = findNameFor(arr.items)
                LIST.parameterizedBy(inner)
            }
            SchemaType.Map -> {
                val map = schema as MapSchema
                val inner = findNameFor(map.additionalProperties as Schema<*>)
                MAP.parameterizedBy(STRING, inner)
            }
            SchemaType.Composition -> schema.mapToTypeName()
        }
    }

    private fun findRawNameFor(query: Schema<*>): String? =
        allNamedSchemas.filterValues { schema ->
            query === schema
        }.keys.firstOrNull()

    private fun OpenAPI.getAllSchemas(
        tagFilter: TagFilter = emptySet()
    ): Collection<SchemaWithInfo<*>> {
        val visited = mutableSetOf<SchemaWithInfo<*>>()

        visit(tagFilter) { path, schema ->
            val info = when (schema) {
                is ArraySchema -> ArraySchemaWithInfo(
                    schema = schema,
                    rawName = findRawNameFor(schema) ?: "",
                    schemaType = SchemaType.Array,
                    path = path
                )
                is MapSchema -> MapSchemaWithInfo(
                    schema = schema,
                    rawName = findRawNameFor(schema) ?: "",
                    schemaType = SchemaType.Map,
                    path = path
                )
                else -> SchemaWithInfo(
                    schema = schema,
                    rawName = findRawNameFor(schema) ?: "",
                    schemaType = schema.getSchemaType(),
                    path = path
                )
            }
            visited.add(info)
        }

        return visited
    }
}
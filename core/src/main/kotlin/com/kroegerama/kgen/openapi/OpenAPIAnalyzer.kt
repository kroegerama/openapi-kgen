package com.kroegerama.kgen.openapi

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.language.asTypeName
import com.kroegerama.kgen.model.ModelTree
import com.kroegerama.kgen.model.ModelTreeNode
import com.kroegerama.kgen.model.OperationWithInfo
import com.kroegerama.kgen.model.SchemaWithInfo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityScheme
import java.util.*
import kotlin.collections.HashSet

@Suppress("JoinDeclarationAndAssignment")
class OpenAPIAnalyzer(
    private val openAPI: OpenAPI,
    private val options: OptionSet
) {

    private val allNamedSchemas: Map<String, Schema<*>>

    val namedPrimitives = TreeSet<SchemaWithInfo>()
    val namedArrays = TreeSet<SchemaWithInfo>()
    val namedMaps = TreeSet<SchemaWithInfo>()
    val enums = TreeSet<SchemaWithInfo>()
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
        val rootSchemas = mutableListOf<SchemaWithInfo>()
        val unnamedSchemas = mutableListOf<SchemaWithInfo>()
        val oneOfs = mutableMapOf<SchemaWithInfo, MutableList<SchemaWithInfo>>()

        val ignoredAnonymousTypes = listOf(
            SchemaType.Primitive,
            SchemaType.Array,
            SchemaType.Map
        )

        val removeItems = mutableListOf<SchemaWithInfo>()
        allSchemaInfo.forEach { schemaInfo ->
            val name = schemaInfo.name
            val schemaType = schemaInfo.schemaType

            if (name.isNotEmpty()) {
                when (schemaInfo.schemaType) {
                    SchemaType.Array -> namedArrays.add(schemaInfo)
                    SchemaType.Map -> namedMaps.add(schemaInfo)
                    SchemaType.Primitive -> namedPrimitives.add(schemaInfo)
                    SchemaType.Enum -> enums.add(schemaInfo)
                    SchemaType.Object -> rootSchemas.add(schemaInfo)
                    SchemaType.Ref -> throw IllegalStateException("ref should already be resolved")
                    SchemaType.AllOf -> rootSchemas.add(schemaInfo)
                    SchemaType.OneOf -> {
                        val composed = schemaInfo.schema as ComposedSchema
                        val mapping = composed.discriminator.mapping.orEmpty()
                        composed.oneOf.forEach { childSchema ->
                            val child = allSchemaInfo.firstOrNull { it.schema === childSchema }
                                ?: throw IllegalStateException("could not find schema for oneOf ${schemaInfo.name}")
                            val mappedName = mapping.entries.firstOrNull { (_, value: String) ->
                                value.endsWith("/${child.name}")
                            }?.key ?: child.name
                            child.discriminator = mappedName
                            val children = oneOfs.getOrPut(schemaInfo) { mutableListOf() }
                            children += child
                            removeItems += child
                        }
                    }
                    SchemaType.AnyOf -> rootSchemas.add(schemaInfo)
                }
            } else if (schemaType !in ignoredAnonymousTypes) {
                unnamedSchemas.add(schemaInfo)
            }
        }
        rootSchemas.removeAll(removeItems)

        val root = rootSchemas.map { schemaInfo ->
            ModelTreeNode(schemaInfo, HashSet())
        } + oneOfs.map { (schemaInfo, children) ->
            ModelTreeNode(schemaInfo, children.map { ModelTreeNode(it, HashSet()) }.toMutableSet())
        }
        val tmpTree = ModelTree(root, emptyList())

        var foundItem = true
        while (foundItem) {
            foundItem = false
            val accepted = mutableSetOf<SchemaWithInfo>()

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

        val unnamedEnums = unnamedSchemas.filter {
            it.schemaType == SchemaType.Enum
        }
        unnamedSchemas.removeAll(unnamedEnums)
        val namedEnums = unnamedEnums.map { unnamedEnum ->
            val name = unnamedEnum.path.joinToString(" ")
            unnamedEnum.withName(name)
        }
        enums.addAll(namedEnums)

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

    fun findTypeNameFor(schema: Schema<*>): TypeName {
        val treeName = objectTree.findName(schema)
        if (treeName.isNotEmpty()) {
            return ClassName(options.modelPackage, treeName)
        }

        val knownName = findRawNameFor(schema)
        if (knownName != null) return ClassName(options.modelPackage, knownName.asTypeName())

        val enumName = enums.firstOrNull { it.schema === schema }?.name
        if (enumName != null) return ClassName(options.modelPackage, enumName.asTypeName())


        return when (schema.getSchemaType()) {
            SchemaType.Primitive -> schema.mapToTypeName()
            SchemaType.Enum -> schema.mapToTypeName()
            SchemaType.Array -> {
                val arr = schema as ArraySchema
                val inner = findTypeNameFor(arr.items)
                LIST.parameterizedBy(inner)
            }
            SchemaType.Map -> {
                val map = schema as MapSchema
                val inner = findTypeNameFor(map.additionalProperties as Schema<*>)
                MAP.parameterizedBy(STRING, inner)
            }
            SchemaType.Ref -> {
                val refTypeName = schema.getRefTypeName()
                val refType = openAPI.components.schemas[refTypeName] ?: throw IllegalStateException("Schema not found $refTypeName")
                findTypeNameFor(refType)
            }
            SchemaType.Object,
            SchemaType.AllOf,
            SchemaType.OneOf,
            SchemaType.AnyOf -> throw IllegalStateException()
        }
    }

    private fun findRawNameFor(query: Schema<*>): String? =
        allNamedSchemas.filterValues { schema ->
            query === schema
        }.keys.firstOrNull()

    private fun OpenAPI.getAllSchemas(
        tagFilter: TagFilter = emptySet()
    ): Collection<SchemaWithInfo> {
        val visited = mutableSetOf<SchemaWithInfo>()

        visit(tagFilter) { path, schema ->
            visited.add(
                SchemaWithInfo(
                    schema = schema,
                    rawName = findRawNameFor(schema) ?: "",
                    schemaType = schema.getSchemaType(),
                    path = path
                )
            )
        }

        return visited
    }
}
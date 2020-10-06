package com.kroegerama.kgen.poet

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.language.asClassFileName
import com.kroegerama.kgen.language.asTypeName
import com.kroegerama.kgen.model.ModelTreeNode
import com.kroegerama.kgen.openapi.OpenAPIAnalyzer
import com.kroegerama.kgen.openapi.SchemaType
import com.squareup.kotlinpoet.*
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.MapSchema

interface IModelFilesGenerator {
    fun getModelFiles(): List<FileSpec>
}

class ModelFilesGenerator(
    openAPI: OpenAPI,
    options: OptionSet,
    analyzer: OpenAPIAnalyzer
) : IModelFilesGenerator,
    IPoetGeneratorSchemaHandler by PoetGeneratorSchemaHandler(openAPI, options, analyzer),
    IPoetGeneratorBase by PoetGeneratorBase(openAPI, options, analyzer) {

    override fun getModelFiles() =
        getPrimitivesFile() + getEnumsFile() + getObjectFiles() + getUnnamedObjectFiles()

    private fun getPrimitivesFile(): List<FileSpec> {
        val namedPrimitives = getNamedPrimitives()
        val aliases = getTypeAliases()

        return if (namedPrimitives.size + aliases.size == 0) emptyList() else listOf(
            prepareFileSpec(options.modelPackage, "\$Named") {
                namedPrimitives.forEach { (tSpec, fSpec) ->
                    addType(tSpec)
                    addFunction(fSpec)
                }
                aliases.forEach { ta ->
                    addTypeAlias(ta)
                }
            }
        )
    }

    private fun getEnumsFile() = getEnums().let { enums ->
        if (enums.isEmpty()) {
            emptyList()
        } else {
            listOf(
                prepareFileSpec(options.modelPackage, "\$Enums") {
                    enums.forEach { enum ->
                        addType(enum)
                    }
                }
            )
        }
    }

    private fun getUnnamedObjectFiles(): List<FileSpec> = analyzer.objectTree.unknown.map { schemaInfo ->
        val name = schemaInfo.name
        val schema = schemaInfo.schema

        prepareFileSpec(options.modelPackage, name.asClassFileName()) {
            val className = ClassName(options.modelPackage, name.asTypeName())
            val type = schema.asTypeSpec(className) {}
            addType(type)
        }
    }

    private fun getObjectFiles(): List<FileSpec> = analyzer.objectTree.nodes.map { (schemaInfo, children) ->
        val name = schemaInfo.name
        val schema = schemaInfo.schema

        prepareFileSpec(options.modelPackage, name.asClassFileName()) {
            val className = ClassName(options.modelPackage, name.asTypeName())
            val rootType = schema.asTypeSpec(className) {
                addChildren(children)
            }

            addType(rootType)
        }
    }

    private fun TypeSpec.Builder.addChildren(
        items: MutableSet<ModelTreeNode>
    ) {
        items.forEach { (schemaInfo, children) ->
            val name = schemaInfo.name
            val schema = schemaInfo.schema

            val className = ClassName(options.modelPackage, name.asTypeName())

            val type = when (val type = schemaInfo.schemaType) {
                SchemaType.Object -> schema.asTypeSpec(className) { addChildren(children) }
                SchemaType.Enum -> schema.asEnumSpec(className)
                SchemaType.Composition -> TODO()
                else -> throw IllegalStateException("Type $type not allowed in ModelTree")
            }
            addType(type)
        }
    }

    private fun getNamedPrimitives(): List<Pair<TypeSpec, FunSpec>> = mutableListOf<Pair<TypeSpec, FunSpec>>().apply {
        if (options.useInlineClass) {
            analyzer.namedPrimitives.forEach { schemaInfo ->
                val schema = schemaInfo.schema
                val name = ClassName(options.modelPackage, schemaInfo.name.asTypeName())
                add(schema.createNamedPrimitive(name))
            }
        }
    }

    private fun getTypeAliases(): List<TypeAliasSpec> = mutableListOf<TypeAliasSpec>().apply {
        with(analyzer) {
            if (!options.useInlineClass) {
                namedPrimitives.forEach { schemaInfo ->
                    val schema = schemaInfo.schema
                    val name = schemaInfo.name
                    add(schema.createPrimitiveTypeAlias(name))
                }
            }
            namedArrays.forEach { schemaInfo ->
                val schema = schemaInfo.schema
                val name = schemaInfo.name
                add((schema as ArraySchema).createArrayTypeAlias(name))
            }
            namedMaps.forEach { schemaInfo ->
                val schema = schemaInfo.schema
                val name = schemaInfo.name
                add((schema as MapSchema).createMapTypeAlias(name))
            }
        }
    }

    private fun getEnums(): List<TypeSpec> = mutableListOf<TypeSpec>().apply {
        with(analyzer.enums) {
            forEach { schemaInfo ->
                val schema = schemaInfo.schema
                val name = schemaInfo.name
                val className = ClassName(options.modelPackage, name.asTypeName())
                add(schema.asEnumSpec(className))
            }
        }
    }
}
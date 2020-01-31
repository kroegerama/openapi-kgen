package com.kroegerama.retrokotgen.poet

import com.kroegerama.retrokotgen.language.asClassFileName
import com.kroegerama.retrokotgen.language.asTypeName
import com.kroegerama.retrokotgen.model.ModelTreeNode
import com.kroegerama.retrokotgen.openapi.SchemaType
import com.squareup.kotlinpoet.*

interface IModelFilesGenerator {
    fun getModelFiles(): List<FileSpec>
}

object ModelFilesGenerator :
    IModelFilesGenerator,
    IPoetGeneratorSchemaHandler by PoetGeneratorSchemaHandler,
    IPoetGeneratorBase by PoetGeneratorBase {

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
                addChildren(this, children)
            }

            addType(rootType)
        }
    }

    private fun addChildren(
        builder: TypeSpec.Builder,
        items: MutableSet<ModelTreeNode>
    ) {
        items.forEach { (schemaInfo, children) ->
            val name = schemaInfo.name
            val schema = schemaInfo.schema

            val className = ClassName(options.modelPackage, name.asTypeName())

            val type = when (val type = schemaInfo.schemaType) {
                SchemaType.Object -> schema.asTypeSpec(className) { addChildren(this, children) }
                SchemaType.Enum -> schema.asEnumSpec(className)
                SchemaType.Composition -> TODO()
                else -> throw IllegalStateException("Type $type not allowed in ModelTree")
            }
            builder.addType(type)
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
                add(schema.createArrayTypeAlias(name))
            }
            namedMaps.forEach { schemaInfo ->
                val schema = schemaInfo.schema
                val name = schemaInfo.name
                add(schema.createMapTypeAlias(name))
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
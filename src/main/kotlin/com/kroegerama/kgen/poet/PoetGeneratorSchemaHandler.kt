package com.kroegerama.kgen.poet

import com.kroegerama.kgen.language.asConstantName
import com.kroegerama.kgen.language.asFieldName
import com.kroegerama.kgen.language.asFunctionName
import com.kroegerama.kgen.language.asTypeName
import com.kroegerama.kgen.openapi.SchemaType
import com.kroegerama.kgen.openapi.getSchemaType
import com.kroegerama.kgen.openapi.isNullable
import com.kroegerama.kgen.openapi.mapToTypeName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema

interface IPoetGeneratorSchemaHandler {
    fun Schema<*>.createNamedPrimitive(name: ClassName): Pair<TypeSpec, FunSpec>
    fun Schema<*>.createPrimitiveTypeAlias(name: String): TypeAliasSpec
    fun ArraySchema.createArrayTypeAlias(name: String): TypeAliasSpec
    fun MapSchema.createMapTypeAlias(name: String): TypeAliasSpec
    fun Schema<*>.asEnumSpec(className: ClassName): TypeSpec
    fun Schema<*>.asTypeSpec(className: ClassName, block: TypeSpec.Builder.() -> Unit): TypeSpec
    fun Schema<*>.convertToParameters(required: Boolean, isMultipart: Boolean): List<ParameterSpecPair>
}

object PoetGeneratorSchemaHandler :
    IPoetGeneratorSchemaHandler,
    IPoetGeneratorBase by PoetGeneratorBase {

    override fun Schema<*>.createNamedPrimitive(name: ClassName): Pair<TypeSpec, FunSpec> {
        val typeName = mapToTypeName()

        val tSpec = poetClass(name) {
            addModifiers(KModifier.INLINE)
            val prop = poetProperty("value", typeName) {
                description?.let { addKdoc(it) }
            }
            primaryConstructor(prop)
            addAnnotation(createJsonClassAnnotation())
        }
        val fSpec = poetFunSpec(name.simpleName.asFunctionName()) {
            receiver(typeName)
            addStatement("return %T(this)", name)
        }
        return tSpec to fSpec
    }

    override fun Schema<*>.createPrimitiveTypeAlias(name: String) =
        poetTypeAlias(name.asTypeName(), mapToTypeName()) {
            description?.let { addKdoc(it) }
        }

    override fun ArraySchema.createArrayTypeAlias(name: String): TypeAliasSpec {
        val innerType = analyzer.findNameFor(items)
        val listType = LIST.parameterizedBy(innerType)

        return poetTypeAlias(name.asTypeName(), listType) {
            description?.let { addKdoc(it) }
        }
    }

    override fun MapSchema.createMapTypeAlias(name: String): TypeAliasSpec {
        val valueSchema = additionalProperties as? Schema<*>
        val valueType = valueSchema?.let {
            analyzer.findNameFor(valueSchema)
        } ?: ANY
        val mapType = MAP.parameterizedBy(STRING, valueType)

        return poetTypeAlias(name.asTypeName(), mapType) {
            description?.let { addKdoc(it) }
        }
    }

    override fun Schema<*>.asEnumSpec(className: ClassName) = poetEnum(className) {
        enum.orEmpty().forEach { value ->
            val valueName = value.toString().asConstantName()

            addEnumConstant(valueName, poetAnonymousClass {
                addAnnotation(createJsonAnnotation(value.toString()))
                description?.let { addKdoc(it) }
            })
        }
    }

    override fun Schema<*>.asTypeSpec(className: ClassName, block: TypeSpec.Builder.() -> Unit) = poetClass(className) {
        addModifiers(KModifier.DATA)

        description?.let { addKdoc("%L\n", it) }

        val propSpecs = properties.orEmpty().map { (propertyName, propertySchema) ->
            val fieldName = propertyName.asFieldName()
            val isNullable = isNullable(required.orEmpty().contains(propertyName), propertySchema.nullable)
            val fieldType = analyzer.findNameFor(propertySchema).nullable(isNullable)

            propertySchema.description?.let {
                addKdoc("@param %L %L\n", fieldName, it)
            }

            poetProperty(fieldName, fieldType) {
                addAnnotation(createJsonAnnotation(propertyName))
            }
        }
        primaryConstructor(*propSpecs.toTypedArray())
        apply(block)
        addAnnotation(createJsonClassAnnotation())
    }

    override fun Schema<*>.convertToParameters(required: Boolean, isMultipart: Boolean): List<ParameterSpecPair> {
        val type = getSchemaType()
        if (type != SchemaType.Object) throw IllegalStateException("Multipart and URL encoded are only supported with Object as Content Type")

        return properties.orEmpty().map { (propertyName, propertySchema) ->
            val propertyNullable = isNullable(this, propertyName, propertySchema)

            val typeName = when (propertySchema) {
                is BinarySchema -> PoetConstants.OK_REQUEST_BODY
                is ArraySchema -> LIST.parameterizedBy(PoetConstants.OK_REQUEST_BODY)
                else -> analyzer.findNameFor(propertySchema)
            }.nullable(!required || propertyNullable)

            val ifaceParam = poetParameter(propertyName.asFieldName(), typeName) {
                val annotation = if (isMultipart) createPartAnnotation(propertyName) else createFieldAnnotation(propertyName)
                addAnnotation(annotation)
            }
            val delegateParam = poetParameter(propertyName.asFieldName(), typeName) {
                if (!required || propertyNullable) defaultValue("null")
                propertySchema.description?.let { addKdoc("%L", it) }
            }
            ParameterSpecPair(ifaceParam, delegateParam)
        }
    }

}
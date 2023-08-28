package com.kroegerama.kgen.poet

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.language.asConstantName
import com.kroegerama.kgen.language.asFieldName
import com.kroegerama.kgen.language.asFunctionName
import com.kroegerama.kgen.language.asTypeName
import com.kroegerama.kgen.openapi.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.*

interface IPoetGeneratorSchemaHandler {
    fun Schema<*>.createNamedPrimitive(name: ClassName): Pair<TypeSpec, FunSpec>
    fun Schema<*>.createPrimitiveTypeAlias(name: String): TypeAliasSpec
    fun ArraySchema.createArrayTypeAlias(name: String): TypeAliasSpec
    fun MapSchema.createMapTypeAlias(name: String): TypeAliasSpec
    fun Schema<*>.asEnumSpec(className: ClassName): TypeSpec
    fun Schema<*>.asTypeSpec(className: ClassName, block: TypeSpec.Builder.() -> Unit): TypeSpec
    fun Schema<*>.asSealedTypeSpec(className: ClassName, block: TypeSpec.Builder.() -> Unit): TypeSpec
    fun Schema<*>.convertToParameters(required: Boolean, isMultipart: Boolean): List<ParameterSpecPairInfo>
}

class PoetGeneratorSchemaHandler(
    openAPI: OpenAPI,
    options: OptionSet,
    analyzer: OpenAPIAnalyzer
) : IPoetGeneratorSchemaHandler,
    IPoetGeneratorBase by PoetGeneratorBase(openAPI, options, analyzer) {

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
        val innerType = analyzer.findTypeNameFor(items)
        val listType = LIST.parameterizedBy(innerType)

        return poetTypeAlias(name.asTypeName(), listType) {
            description?.let { addKdoc(it) }
        }
    }

    override fun MapSchema.createMapTypeAlias(name: String): TypeAliasSpec {
        val valueSchema = additionalProperties as? Schema<*>
        val valueType = valueSchema?.let {
            analyzer.findTypeNameFor(valueSchema)
        } ?: ANY
        val mapType = MAP.parameterizedBy(STRING, valueType)

        return poetTypeAlias(name.asTypeName(), mapType) {
            description?.let { addKdoc(it) }
        }
    }

    override fun Schema<*>.asEnumSpec(className: ClassName) = poetEnum(className) {
        val extensions = this@asEnumSpec.extensions

        var enumNames = extensions.getOrDefault("x-enumNames", null) as? ArrayList<String>
        enumNames = enumNames ?: extensions.getOrDefault("x-enum-varnames", null) as? ArrayList<String>

        if (enumNames == null || enumNames.count() != enum?.count()) {
            enumNames = ArrayList<String>().apply {
                addAll(enum.orEmpty().map { it.toString().asConstantName() })
            }
        }

        enum.orEmpty().forEachIndexed { idx, value ->
            val valueName = enumNames[idx];

            addEnumConstant(valueName, poetAnonymousClass {
                addAnnotation(createJsonAnnotation(value.toString()))
                description?.let { addKdoc(it) }
            })
        }
    }

    override fun Schema<*>.asSealedTypeSpec(className: ClassName, block: TypeSpec.Builder.() -> Unit) = poetClass(className) {
        addModifiers(KModifier.SEALED)
        description?.let { addKdoc("%L\n", it) }

        apply(block)
        val discriminator = (this@asSealedTypeSpec as ComposedSchema).discriminator.propertyName
        addAnnotation(createJsonClassAnnotation(discriminator))
    }

    override fun Schema<*>.asTypeSpec(className: ClassName, block: TypeSpec.Builder.() -> Unit) = poetClass(className) {
        description?.let { addKdoc("%L\n", it) }

        val allProperties = mutableMapOf<String, Schema<*>>()
        properties?.let(allProperties::putAll)

        if (this@asTypeSpec is ComposedSchema) {
            //allOf is handled by properties, because
            //isFlattenComposedSchemas == true

            //TODO: Better handling of anyOf/oneOf -> e.g. use discriminator
            anyOf?.mapNotNull(Schema<*>::getProperties)?.forEach(allProperties::putAll)
//            oneOf?.mapNotNull(Schema<*>::getProperties)?.forEach(allProperties::putAll)
        }

        val propSpecs = allProperties.map { (propertyName, propertySchema) ->
            val fieldName = propertyName.asFieldName()
            val isNullable = isNullable(required.orEmpty().contains(propertyName), propertySchema.nullable)
            val fieldType = analyzer.findTypeNameFor(propertySchema).nullable(isNullable)

            propertySchema.description?.let {
                addKdoc("@param %L %L\n", fieldName, it)
            }

            val isMutable = propertySchema.extensions.orEmpty()["x-kgen-variable"] as? Boolean == true
            poetProperty(fieldName, fieldType) {
                mutable(isMutable)
                addAnnotation(createJsonAnnotation(propertyName))
            }
        }
        primaryConstructor(*propSpecs.toTypedArray())
        apply(block)
        addAnnotation(createJsonClassAnnotation())

        if (propSpecs.isNotEmpty()) {
            addModifiers(KModifier.DATA)
        }
    }

    override fun Schema<*>.convertToParameters(required: Boolean, isMultipart: Boolean): List<ParameterSpecPairInfo> {
        val type = getSchemaType()
        if (type != SchemaType.Object) throw IllegalStateException("Multipart and URL encoded are only supported with Object as Content Type")

        return properties.orEmpty().map { (propertyName, propertySchema) ->
            val propertyNullable = isNullable(this, propertyName, propertySchema)

            val typeName = when (propertySchema) {
                is BinarySchema -> if (isMultipart) PoetConstants.OK_MULTIPART_PART else PoetConstants.OK_REQUEST_BODY
                is ArraySchema -> LIST.parameterizedBy(PoetConstants.OK_REQUEST_BODY)
                else -> analyzer.findTypeNameFor(propertySchema)
            }.nullable(!required || propertyNullable)

            val ifaceParam = poetParameter(propertyName.asFieldName(), typeName) {
                val annotationValue = if (propertySchema is BinarySchema) null else propertyName
                val annotation = if (isMultipart) createPartAnnotation(annotationValue) else createFieldAnnotation(propertyName)
                addAnnotation(annotation)
            }
            val delegateParam = poetParameter(propertyName.asFieldName(), typeName) {
                if (!required || propertyNullable) defaultValue("null")
                propertySchema.description?.let { addKdoc("%L", it) }
            }
            ParameterSpecPairInfo(ifaceParam, delegateParam)
        }
    }

}

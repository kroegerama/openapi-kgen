package com.kroegerama.kgen.spec

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.asBaseUrl
import com.kroegerama.kgen.language.asTypeName
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.servers.Server

fun Schema<*>.getSpecType(): SpecSchemaType {
    if (`$ref` != null) return SpecSchemaType.Ref
    if (additionalProperties == true) return SpecSchemaType.Raw
    if (additionalProperties is Schema<*>) return SpecSchemaType.Map
    val resolvedType = resolveType()

    if (resolvedType == "array") return SpecSchemaType.Array
    if (resolvedType == "string" && !enum.isNullOrEmpty()) return SpecSchemaType.Enum

    if (resolvedType in primitiveTypes) {
        val primitiveType = when (resolvedType) {
            "integer" -> when (format) {
                "int32" -> SpecPrimitiveType.Int32
                "int64" -> SpecPrimitiveType.Int64
                "float" -> SpecPrimitiveType.Float
                "double" -> SpecPrimitiveType.Double
                else -> SpecPrimitiveType.Int32
            }

            "number" -> when (format) {
                "int32" -> SpecPrimitiveType.Int32
                "int64" -> SpecPrimitiveType.Int64
                "float" -> SpecPrimitiveType.Float
                "double" -> SpecPrimitiveType.Double
                else -> SpecPrimitiveType.Float
            }

            "string" -> when (format) {
                "date" -> SpecPrimitiveType.Date
                "time" -> SpecPrimitiveType.Time
                "date-time" -> SpecPrimitiveType.DateTime
                "base64" -> SpecPrimitiveType.Base64
                "byte" -> SpecPrimitiveType.Base64
                "uuid" -> SpecPrimitiveType.UUID
                else -> SpecPrimitiveType.String
            }

            "boolean" -> SpecPrimitiveType.Boolean

            else -> throw IllegalStateException("type $resolvedType in $primitiveTypes, but case is missing")
        }
        return SpecSchemaType.Primitive(
            type = primitiveType
        )
    }

    return when {
        !oneOf.isNullOrEmpty() -> SpecSchemaType.Sealed
        !anyOf.isNullOrEmpty() -> SpecSchemaType.Object
        !allOf.isNullOrEmpty() -> SpecSchemaType.Object
        properties.isNullOrEmpty() -> SpecSchemaType.Raw
        else -> SpecSchemaType.Object
    }
}

fun Schema<*>.isNullable(required: Boolean?): Boolean {
    if (nullable != null) return nullable
    if (types != null && "null" in types) return true
    if (required != null) return !required
    return false
}

fun Schema<*>.fullDescription() = listOfNotNull(
    description,
    example?.let { "Example: $it" },
    examples?.joinToString("\n\t", prefix = "Examples:\n\t") { it.toString() }
).joinToString("\n").ifBlank { null }

fun String.refAsTypeNames() =
    removePrefix("#/components/schemas/").split('/').map { it.asTypeName() }

fun Schema<*>.resolveRef(
    spec: OpenAPI
): Schema<*>? {
    val name = `$ref`?.substringAfterLast('/') ?: return null
    val ref = spec.components?.schemas?.get(name)
    require(ref != null) { "cannot resolve $`$ref`" }
    return ref
}

fun Schema<*>.resolveProperties(
    spec: OpenAPI,
): Map<String, Schema<*>> {
    val collectedProperties: MutableMap<String, Schema<*>> = mutableMapOf()
    val visitedSchemas: MutableSet<Int> = mutableSetOf()

    fun Schema<*>.inner() {
        val hash = System.identityHashCode(this)
        if (hash in visitedSchemas) return
        visitedSchemas += hash

        resolveRef(spec)?.let {
            it.inner()
            return
        }
        if (anyOf != null) {
            anyOf.forEach {
                it.inner()
            }
            return
        }
        if (allOf != null) {
            allOf.forEach {
                it.inner()
            }
            return
        }
        if (oneOf != null) {
            return
        }
        properties?.let {
            collectedProperties += it
        }
    }

    inner()

    return collectedProperties
}

private fun Schema<*>.resolveType(): String {
    if (type != null) return type
    types?.forEach {
        if (it in allTypes) {
            return it
        }
    }
    return "object"
}

private val primitiveTypes = setOf(
    "string",
    "integer",
    "number",
    "boolean"
)

private val allTypes = setOf("object", "array") + primitiveTypes

fun Operation.resolveTags() = tags.orEmpty().ifEmpty { listOf(Constants.FALLBACK_TAG) }

private val pathParamRegex = """[{](\S+?)[}]""".toRegex()

fun Server.asBaseUrl(): String {
    val resolvedUrl = url.orEmpty().replace(pathParamRegex) { r ->
        val key = r.groupValues[1]
        variables?.get(key)?.run {
            default ?: enum?.firstOrNull()
        } ?: r.value
    }
    return resolvedUrl.asBaseUrl()
}

package com.kroegerama.kgen.openapi

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.core.models.ParseOptions

fun parseSpecFile(specFile: String, allowParseErrors: Boolean): OpenAPI {
    val parseOpts = ParseOptions().apply {
        isResolve = true
        isFlatten = true
        isResolveFully = true
        isFlattenComposedSchemas = true
        isResolveCombinators = true
    }

    val result = OpenAPIParser().readLocation(specFile, emptyList(), parseOpts)
    if (!allowParseErrors && (!result.messages.isNullOrEmpty() || result.openAPI == null)) {
        throw IllegalStateException(
            "Parsing error: ${
                result.messages.orEmpty().joinToString("\n", prefix = "[\n", postfix = "\n]")
            }"
        )
    }
    return result.openAPI
}

fun Schema<*>.getRefTypeName(): String? = `$ref`?.substringAfterLast('/')

enum class SchemaType(
    val shortName: String
) {
    Primitive("Primitive"),
    Object("Obj"),
    Enum("Enum"),
    Array("Arr"),
    Map("Map"),
    AllOf("AllOf"),
    OneOf("OneOf"),
    AnyOf("AnyOf"),
    Ref("Ref")
}

enum class OperationRequestType {
    Default,
    Multipart,
    UrlEncoded,
    Unknown
}

enum class ParameterType {
    Cookie,
    Header,
    Path,
    Query
}

enum class SecurityType {
    Basic,
    Bearer,
    Header,
    Query,
    OAuth,
    Unknown
}

/**
 * We model absent properties as nullable -> not required and not nullable will result in nullable for now
 * Default required = false (name is not in required[])
 * Default nullable = true (nullable = null)
 *
 *      | required | nullable | isNullable |
 *      |:--------:|:--------:|:----------:|
 *      | false    | null     | true       |
 *      | false    | false    | true       |
 *      | false    | true     | true       |
 *      | true     | null     | false      |
 *      | true     | false    | false      |
 *      | true     | true     | true       |
 */
fun isNullable(required: Boolean?, nullable: Boolean?): Boolean {
    val isRequired = required ?: false
    val isNullable = nullable ?: false

    return !isRequired || isNullable
}

fun isNullable(parent: Schema<*>, childName: String, child: Schema<*>) =
    isNullable(parent.required?.contains(childName), child.nullable)
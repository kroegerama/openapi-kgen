package com.kroegerama.kgen.openapi

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.core.models.ParseOptions

fun parseSpecFile(specFile: String): OpenAPI {
    val parseOpts = ParseOptions().apply {
        isResolve = true
        isFlatten = true
        isResolveFully = true
        isFlattenComposedSchemas = true
        isResolveCombinators = true
    }

    return OpenAPIParser().readLocation(specFile, emptyList(), parseOpts).openAPI
}

enum class SchemaType(
    val shortName: String
) {
    Primitive("Primitive"),
    Object("Obj"),
    Enum("Enum"),
    Array("Arr"),
    Map("Map"),
    Composition("Comp")
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
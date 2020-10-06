package com.kroegerama.kgen.openapi

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.poet.PoetConstants
import com.squareup.kotlinpoet.*
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.*
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.parser.util.SchemaTypeUtil

fun OpenAPI.getAllNamedSchemas(): Map<String, Schema<*>> {
    val schemas = components?.schemas.orEmpty()
    val params = components?.parameters.orEmpty().mapValues { (_, param) -> param.schema }
    return schemas + params
}

fun OpenAPI.findSecurityScheme(name: String): SecurityScheme? =
    components?.securitySchemes?.getOrDefault(name, null)

fun Schema<*>.mapToTypeName(): ClassName = when (this) {
    is StringSchema -> STRING
    is IntegerSchema -> when (format) {
        null -> INT
        SchemaTypeUtil.INTEGER32_FORMAT -> INT
        SchemaTypeUtil.INTEGER64_FORMAT -> LONG
        else -> throw IllegalStateException("Integer format not allowed: $format")
    }
    is NumberSchema -> when (format) {
        null -> FLOAT
        SchemaTypeUtil.FLOAT_FORMAT -> FLOAT
        SchemaTypeUtil.DOUBLE_FORMAT -> DOUBLE
        else -> throw IllegalStateException("Number format not allowed: $format")
    }
    is BooleanSchema -> BOOLEAN

    is BinarySchema -> BYTE_ARRAY
    is ByteArraySchema -> BYTE_ARRAY
    is FileSchema -> TODO()

    is EmailSchema -> STRING
    is PasswordSchema -> STRING
    is UUIDSchema -> STRING

    is DateSchema -> PoetConstants.DATE
    is DateTimeSchema -> PoetConstants.DATE

    else -> throw IllegalStateException("Schema not supported: ${this.javaClass.simpleName} (type: ${this.type}, format: ${this.format})")
}

fun Schema<*>.getSchemaType() = when {
    !enum.isNullOrEmpty() -> SchemaType.Enum
    this.additionalProperties is MapSchema -> SchemaType.Map
    this.additionalProperties == true -> SchemaType.Map

    this is StringSchema -> SchemaType.Primitive
    this is IntegerSchema -> SchemaType.Primitive
    this is NumberSchema -> SchemaType.Primitive
    this is BooleanSchema -> SchemaType.Primitive

    this is BinarySchema -> SchemaType.Primitive
    this is ByteArraySchema -> SchemaType.Primitive
    this is FileSchema -> SchemaType.Primitive

    this is EmailSchema -> SchemaType.Primitive
    this is PasswordSchema -> SchemaType.Primitive
    this is UUIDSchema -> SchemaType.Primitive

    this is DateSchema -> SchemaType.Primitive
    this is DateTimeSchema -> SchemaType.Primitive

    this is ArraySchema -> SchemaType.Array
    this is MapSchema -> SchemaType.Map
    this is ObjectSchema -> SchemaType.Object
    this is ComposedSchema -> when {
        allOf != null -> SchemaType.AllOf
        oneOf != null -> SchemaType.OneOf
        anyOf != null -> SchemaType.AnyOf
        else -> SchemaType.Object
    }

    `$ref` != null -> SchemaType.Ref
    else -> SchemaType.Object
}

fun PathItem.HttpMethod.mapToName(): ClassName = when (this) {
    PathItem.HttpMethod.POST -> PoetConstants.RETROFIT_POST
    PathItem.HttpMethod.GET -> PoetConstants.RETROFIT_GET
    PathItem.HttpMethod.PUT -> PoetConstants.RETROFIT_PUT
    PathItem.HttpMethod.PATCH -> PoetConstants.RETROFIT_PATCH
    PathItem.HttpMethod.DELETE -> PoetConstants.RETROFIT_DELETE
    PathItem.HttpMethod.HEAD -> PoetConstants.RETROFIT_HEAD
    PathItem.HttpMethod.OPTIONS -> PoetConstants.RETROFIT_OPTIONS
    PathItem.HttpMethod.TRACE -> PoetConstants.RETROFIT_TRACE
}

fun String.mapMimeToRequestType(): OperationRequestType = when (this) {
    Constants.MIME_TYPE_JSON -> OperationRequestType.Default
    Constants.MIME_TYPE_MULTIPART_FORM_DATA -> OperationRequestType.Multipart
    Constants.MIME_TYPE_URL_ENCODED -> OperationRequestType.UrlEncoded
    else -> OperationRequestType.Unknown
}

fun Parameter.mapToParameterType() = when (this) {
    is CookieParameter -> ParameterType.Cookie
    is HeaderParameter -> ParameterType.Header
    is PathParameter -> ParameterType.Path
    is QueryParameter -> ParameterType.Query
    else -> throw IllegalStateException("Parameter type not supported: $this")
}

fun ParameterType.mapToTypeName() = when (this) {
    ParameterType.Cookie -> throw IllegalStateException("Cookie parameter not supported")
    ParameterType.Header -> PoetConstants.RETROFIT_PARAM_HEADER
    ParameterType.Path -> PoetConstants.RETROFIT_PARAM_PATH
    ParameterType.Query -> PoetConstants.RETROFIT_PARAM_QUERY
}

fun SecurityScheme.mapToType(): SecurityType = when (type) {
    SecurityScheme.Type.HTTP -> when (scheme) {
        "basic" -> SecurityType.Basic
        "bearer" -> SecurityType.Bearer
        else -> SecurityType.Unknown
    }
    SecurityScheme.Type.APIKEY -> when (`in`) {
        SecurityScheme.In.HEADER -> SecurityType.Header
        SecurityScheme.In.QUERY -> SecurityType.Query
        else -> SecurityType.Unknown
    }
    else -> SecurityType.Unknown
}
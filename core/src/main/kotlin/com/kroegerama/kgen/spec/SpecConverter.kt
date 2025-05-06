package com.kroegerama.kgen.spec

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.language.asFieldName
import com.kroegerama.kgen.language.asFunctionName
import com.kroegerama.kgen.language.asTypeName
import com.kroegerama.kgen.spec.SpecOperation.SchemaInfo
import com.kroegerama.kgen.spec.SpecSchema.SimpleType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Discriminator
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.*
import io.swagger.v3.oas.models.security.SecurityScheme
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

typealias SchemaNameEvaluator = (List<String>) -> List<String>
typealias SchemaEmitter = (SpecSchema.NamedSpecSchema) -> Unit

class SpecConverter(
    private val spec: OpenAPI,
    private val options: OptionSet
) {

    private val modelSerialNames = mutableMapOf<List<String>, String>()
    private val modelInterfaces = mutableMapOf<List<String>, MutableList<List<String>>>()

    fun convert(): SpecModel {
        val info = spec.info
        val createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)

        val fileHeader = with(info) {
            buildString {
                appendLine(Constants.FILE_HEADER_NOTE)
                appendLine()
                appendLine(title)
                description?.let { d -> d.split("\n").forEach { appendLine(it) } }
                appendLine("Version $version")
                contact?.run {
                    appendLine()
                    appendLine("Contact")
                    name?.let { appendLine("  $it") }
                    email?.let { appendLine("  Mail: $it") }
                    url?.let { appendLine("  URL: $it") }
                }
                license?.run {
                    appendLine()
                    appendLine("Spec License")
                    name?.let { appendLine("  $it") }
                    url?.let { appendLine("  $it") }
                }
                appendLine()
                val formattedDateTime = createdAt.format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                appendLine("Generated $formattedDateTime")
                appendLine(Constants.generatorInfo)
            }
        }

        return SpecModel(
            metadata = convertMetadata(info, createdAt),
            fileHeader = fileHeader,
            apis = convertApis(),
            schemas = convertSchemas(),
            modelSerialNames = modelSerialNames,
            modelInterfaces = modelInterfaces,
            securitySchemes = convertSecuritySchemes()
        )
    }

    private fun convertMetadata(
        info: Info?,
        createdAt: OffsetDateTime
    ): SpecMetadata {
        val specServers = spec.servers.orEmpty()
        val servers = specServers.map { server -> server.asBaseUrl() }

        return SpecMetadata(
            title = info?.title ?: "<undefined>",
            description = info?.description,
            version = info?.version ?: "0.0.0",
            servers = servers,
            createdAt = createdAt
        )
    }

    private fun convertSecuritySchemes(): List<SpecSecurityScheme> {
        return spec.components?.securitySchemes.orEmpty().mapNotNull { (securitySchemeName, securityScheme) ->
            val type = when (securityScheme.type) {
                SecurityScheme.Type.APIKEY -> when (securityScheme.`in`) {
                    SecurityScheme.In.HEADER -> SpecSecurityScheme.Type.Header
                    SecurityScheme.In.QUERY -> SpecSecurityScheme.Type.Query
                    SecurityScheme.In.COOKIE -> SpecSecurityScheme.Type.Cookie
                    else -> return@mapNotNull null
                }

                SecurityScheme.Type.HTTP -> when (securityScheme.scheme) {
                    "bearer" -> SpecSecurityScheme.Type.Bearer
                    "basic" -> SpecSecurityScheme.Type.Basic
                    else -> return@mapNotNull null
                }

                null,
                SecurityScheme.Type.OAUTH2,
                SecurityScheme.Type.OPENIDCONNECT,
                SecurityScheme.Type.MUTUALTLS -> return@mapNotNull null // not supported yet
            }

            SpecSecurityScheme(
                name = securitySchemeName.asTypeName(),
                rawName = securitySchemeName,
                propertyName = securityScheme.name,
                type = type,
                description = securityScheme.description
            )
        }
    }

    private fun convertSchemas(): List<SpecSchema.NamedSpecSchema> {
        val schemas = mutableListOf<SpecSchema.NamedSpecSchema>()
        spec.components?.schemas?.forEach { (name, schema) ->
            val typeName = name.asTypeName()
            val converted = convertNamedSchema(
                typeNames = listOf(typeName),
                schema = schema,
                schemaNameEvaluator = { names ->
                    listOf(names.joinToString(""))
                },
                schemaEmitter = { schemas += it }
            )
            schemas += converted
        }
        return schemas
    }

    private fun convertApis(): List<SpecApi> {
        val apis = mutableMapOf<String, MutableList<SpecOperation>>()

        spec.paths?.forEach { (path, pathItem) ->
            pathItem.readOperationsMap().forEach { (method, operation) ->
                val tags = operation.resolveTags()
                val specOperation = convertOperation(path, pathItem, method, operation)
                tags.forEach { tag ->
                    apis.getOrPut(tag.asTypeName(), ::mutableListOf) += specOperation
                }
            }
        }
        return apis.map { (name, operations) ->
            SpecApi(
                name = name,
                operations = operations
            )
        }
    }

    private fun convertOperation(path: String, pathItem: PathItem, method: PathItem.HttpMethod, operation: Operation): SpecOperation {
        val name = operation.operationId ?: buildString {
            append(method.name)
            append(".")
            append(path)
        }

        val allParameters = (pathItem.parameters.orEmpty() + operation.parameters.orEmpty()).map {
            convertParameter(it)
        }.groupBy { parameter ->
            parameter.name
        }.flatMap { (_, parameters) ->
            if (parameters.size > 1) {
                parameters.map { parameter ->
                    parameter.copy(
                        name = (parameter.type.name + "." + parameter.name).asFieldName()
                    )
                }
            } else {
                parameters
            }
        }.sortedBy {
            it.type.priority
        }

        val requestBodyEntry = operation.requestBody?.let { requestBody ->
            val contentTypes = requestBody.content.orEmpty().entries
            contentTypes.firstOrNull { (mime, _) ->
                mime in preferredMimes
            } ?: contentTypes.firstOrNull()
        }
        val requestBodyType = requestBodyEntry?.key?.let(::convertMimeType) ?: SpecOperation.Type.Default
        val request = requestBodyEntry?.let { (_, mediaType) ->
            val schema = mediaType.schema
            SchemaInfo(
                type = resolveSchema(schema),
                nullable = operation.requestBody?.required != true,
                description = operation.requestBody?.description
            )
        }

        val response = operation.responses?.let {
            val (_, response) = it.entries.minByOrNull { (code, _) ->
                code.responseCodePriority
            } ?: return@let null
            val description = response.description
            val contentEntries = response.content.orEmpty().entries

            val entry = contentEntries.firstOrNull { (mime, _) ->
                mime == Constants.MIME_TYPE_JSON
            } ?: contentEntries.firstOrNull()

            if (entry != null) {
                val (mime, mediaType) = entry
                val schema = mediaType.schema
                val type = if (mime == Constants.MIME_TYPE_JSON) {
                    resolveSchema(schema)
                } else {
                    SpecSchema.Raw
                }
                SchemaInfo(
                    type = type,
                    nullable = schema.isNullable(null),
                    description = description
                )
            } else {
                SchemaInfo(
                    type = SpecSchema.Unit,
                    nullable = false,
                    description = description
                )
            }
        } ?: SchemaInfo(
            type = SpecSchema.Unit,
            nullable = false,
            description = null
        )

        val securityIds = operation.security.orEmpty().ifEmpty {
            spec.security.orEmpty()
        }.flatMap { it.keys }.distinct().map {
            it.asTypeName()
        }

        val serverOverride = when {
            operation.servers != null -> operation.servers.firstOrNull()?.asBaseUrl()
            pathItem.servers != null -> pathItem.servers.firstOrNull()?.asBaseUrl()
            else -> null
        }

        return SpecOperation(
            name = name.asFunctionName(),
            method = method,
            type = requestBodyType,
            serverOverride = serverOverride,
            path = path,
            parameters = allParameters,
            body = request,
            response = response,
            deprecated = operation.deprecated ?: false,
            securityIds = securityIds
        )
    }

    private fun convertParameter(parameter: Parameter): SpecParameter {
        return SpecParameter(
            name = parameter.name.asFieldName(),
            rawName = parameter.name,
            type = convertParameterType(parameter),
            schema = resolveSchema(parameter.schema),
            nullable = parameter.required != true,
            description = parameter.description
        )
    }

    private fun resolveSchema(schema: Schema<*>): SimpleType = when (val type = schema.getSpecType()) {
        SpecSchemaType.Raw -> SpecSchema.AnyComplex
        SpecSchemaType.Ref -> SpecSchema.Ref(
            typeNames = schema.`$ref`.refAsTypeNames()
        )

        is SpecSchemaType.Primitive -> SpecSchema.Primitive(
            type = type.type
        )

        SpecSchemaType.Array -> SpecSchema.Array(
            items = resolveSchema(schema.items),
            itemsNullable = schema.items.isNullable(null)
        )

        SpecSchemaType.Map -> {
            val mapItemsSchema = schema.additionalProperties as Schema<*>
            SpecSchema.Map(
                items = resolveSchema(mapItemsSchema),
                itemsNullable = mapItemsSchema.isNullable(null)
            )
        }

        SpecSchemaType.Enum,
        SpecSchemaType.Object,
        SpecSchemaType.Sealed -> throw IllegalStateException("cannot resolve schema of type $type - parameter was not flattened")
    }

    private fun convertParameterType(parameter: Parameter): SpecParameter.Type = when (parameter) {
        is CookieParameter -> SpecParameter.Type.Cookie
        is HeaderParameter -> SpecParameter.Type.Header
        is PathParameter -> SpecParameter.Type.Path
        is QueryParameter -> SpecParameter.Type.Query
        else -> throw IllegalStateException("Invalid parameter type $parameter")
    }

    private fun convertMimeType(mime: String): SpecOperation.Type = when (mime) {
        Constants.MIME_TYPE_JSON -> SpecOperation.Type.Default
        Constants.MIME_TYPE_MULTIPART_FORM_DATA -> SpecOperation.Type.Multipart
        Constants.MIME_TYPE_URL_ENCODED -> SpecOperation.Type.UrlEncoded
        else -> SpecOperation.Type.Unknown
    }

    private fun convertNamedSchema(
        typeNames: List<String>,
        schema: Schema<*>,
        schemaNameEvaluator: SchemaNameEvaluator,
        schemaEmitter: SchemaEmitter
    ): SpecSchema.NamedSpecSchema {
        return when (val type = schema.getSpecType()) {
            SpecSchemaType.Raw -> SpecSchema.Typealias(
                typeNames = typeNames,
                deprecated = schema.deprecated ?: false,
                schema = SpecSchema.AnyComplex,
                description = schema.fullDescription()
            )

            SpecSchemaType.Ref -> SpecSchema.Typealias(
                typeNames = typeNames,
                deprecated = schema.deprecated ?: false,
                description = schema.fullDescription(),
                schema = SpecSchema.Ref(
                    typeNames = schema.`$ref`.refAsTypeNames()
                )
            )

            is SpecSchemaType.Primitive -> SpecSchema.Typealias(
                typeNames = typeNames,
                deprecated = schema.deprecated ?: false,
                description = schema.fullDescription(),
                schema = SpecSchema.Primitive(
                    type = type.type
                )
            )

            SpecSchemaType.Array -> SpecSchema.Typealias(
                typeNames = typeNames,
                deprecated = schema.deprecated ?: false,
                description = schema.fullDescription(),
                schema = SpecSchema.Array(
                    items = convertSimpleType(
                        parentTypeNames = typeNames,
                        guessedName = "Items",
                        schema = schema.items,
                        schemaNameEvaluator = schemaNameEvaluator,
                        schemaEmitter = schemaEmitter
                    ),
                    itemsNullable = schema.items.isNullable(null)
                )
            )

            SpecSchemaType.Map -> {
                val mapItemsSchema = schema.additionalProperties as Schema<*>
                SpecSchema.Typealias(
                    typeNames = typeNames,
                    deprecated = schema.deprecated ?: false,
                    description = schema.fullDescription(),
                    schema = SpecSchema.Map(
                        items = convertSimpleType(
                            parentTypeNames = typeNames,
                            guessedName = "Items",
                            schema = mapItemsSchema,
                            schemaNameEvaluator = schemaNameEvaluator,
                            schemaEmitter = schemaEmitter
                        ),
                        itemsNullable = mapItemsSchema.isNullable(null)
                    )
                )
            }

            SpecSchemaType.Enum -> SpecSchema.Enum(
                typeNames = typeNames,
                deprecated = schema.deprecated ?: false,
                description = schema.fullDescription(),
                constants = convertEnumConstants(schema.enum)
            )

            SpecSchemaType.Object -> convertObject(
                typeNames = typeNames,
                schema = schema
            )

            SpecSchemaType.Sealed -> convertSealed(
                typeNames = typeNames,
                schema = schema
            )
        }
    }

    private fun convertSealed(
        typeNames: List<String>,
        schema: Schema<*>
    ): SpecSchema.Sealed {
        val discriminator: Discriminator? = schema.discriminator
        val children = mutableListOf<SpecSchema.NamedSpecSchema>()

        val types: List<SpecSchema.Ref> = schema.oneOf.mapIndexed { index, item ->
            if (item.`$ref` != null) {
                val mappedName = discriminator?.mapping.orEmpty().entries.firstOrNull { (name, ref) ->
                    ref == item.`$ref`
                }?.key ?: item.`$ref`.substringAfterLast('/')

                val refTypeNames = item.`$ref`.refAsTypeNames()
                modelSerialNames[refTypeNames] = mappedName
                modelInterfaces.getOrPut(refTypeNames, ::mutableListOf) += typeNames

                SpecSchema.Ref(
                    typeNames = refTypeNames
                )
            } else {
                val childTypeNames = typeNames + "OneOf$index"
                children += convertNamedSchema(
                    typeNames = childTypeNames,
                    schema = item,
                    schemaNameEvaluator = { it },
                    schemaEmitter = { children += it }
                )
                SpecSchema.Ref(
                    typeNames = childTypeNames
                )
            }
        }
        val discriminatorPropertyName = discriminator?.propertyName ?: "type"

        val sealed = SpecSchema.Sealed(
            typeNames = typeNames,
            deprecated = false,
            discriminator = discriminatorPropertyName,
            types = types,
            children = children,
            description = schema.fullDescription()
        )

        return sealed
    }

    private fun convertObject(
        typeNames: List<String>,
        schema: Schema<*>
    ): SpecSchema.Object {
        val children = mutableListOf<SpecSchema.NamedSpecSchema>()
        val properties = schema.resolveProperties(spec).map { (propertyName, propertySchema) ->
            SpecProperty(
                name = propertyName.asFieldName(),
                rawName = propertyName,
                deprecated = propertySchema.deprecated ?: false,
                nullable = propertySchema.isNullable(
                    schema.required.orEmpty().contains(propertyName)
                ),
                type = convertSimpleType(
                    parentTypeNames = typeNames,
                    guessedName = propertyName.asTypeName(),
                    schema = propertySchema,
                    schemaNameEvaluator = { it },
                    schemaEmitter = { children += it }
                ),
                description = propertySchema.fullDescription()
            )
        }
        return SpecSchema.Object(
            typeNames = typeNames,
            deprecated = schema.deprecated ?: false,
            properties = properties,
            children = children,
            description = schema.fullDescription()
        )
    }

    private fun convertSimpleType(
        parentTypeNames: List<String>,
        guessedName: String,
        schema: Schema<*>,
        schemaNameEvaluator: SchemaNameEvaluator,
        schemaEmitter: SchemaEmitter
    ): SimpleType {
        return when (val type = schema.getSpecType()) {
            SpecSchemaType.Raw -> SpecSchema.AnyComplex

            SpecSchemaType.Ref -> SpecSchema.Ref(
                typeNames = schema.`$ref`.refAsTypeNames()
            )

            is SpecSchemaType.Primitive -> SpecSchema.Primitive(
                type = type.type
            )

            SpecSchemaType.Array -> SpecSchema.Array(
                items = convertSimpleType(
                    parentTypeNames = parentTypeNames,
                    guessedName = guessedName + "Item",
                    schema = schema.items,
                    schemaNameEvaluator = schemaNameEvaluator,
                    schemaEmitter = schemaEmitter
                ),
                itemsNullable = schema.items.isNullable(null)
            )

            SpecSchemaType.Map -> {
                val mapItemsSchema = schema.additionalProperties as Schema<*>
                SpecSchema.Map(
                    items = convertSimpleType(
                        parentTypeNames = parentTypeNames,
                        guessedName = guessedName + "Item",
                        schema = mapItemsSchema,
                        schemaNameEvaluator = schemaNameEvaluator,
                        schemaEmitter = schemaEmitter
                    ),
                    itemsNullable = mapItemsSchema.isNullable(null)
                )
            }

            SpecSchemaType.Enum -> {
                val typeNames = schemaNameEvaluator(parentTypeNames + guessedName)
                val enum = SpecSchema.Enum(
                    typeNames = typeNames,
                    deprecated = schema.deprecated ?: false,
                    constants = convertEnumConstants(schema.enum),
                    description = schema.fullDescription()
                )
                schemaEmitter(enum)
                SpecSchema.Ref(
                    typeNames = typeNames
                )
            }

            SpecSchemaType.Object -> {
                val typeNames = schemaNameEvaluator(parentTypeNames + guessedName)
                val innerObject = convertObject(
                    typeNames = typeNames,
                    schema = schema
                )
                schemaEmitter(innerObject)
                SpecSchema.Ref(
                    typeNames = typeNames
                )
            }

            SpecSchemaType.Sealed -> {
                val typeNames = schemaNameEvaluator(parentTypeNames + guessedName)
                val innerObject = convertSealed(
                    typeNames = typeNames,
                    schema = schema
                )
                schemaEmitter(innerObject)
                SpecSchema.Ref(
                    typeNames = typeNames
                )
            }
        }
    }

    private fun convertEnumConstants(enum: List<*>): List<SpecSchema.Enum.Constant> {
        return enum.map {
            SpecSchema.Enum.Constant(
                value = it.toString(),
                name = it.toString().asTypeName()
            )
        }
    }

    companion object {
        private val String.responseCodePriority: Int
            get() = when (val c = toIntOrNull()) {
                null -> 999
                in 200..299 -> c
                else -> 1000 + c
            }
        private val SpecParameter.Type.priority: Int
            get() = when (this) {
                SpecParameter.Type.Cookie -> 0
                SpecParameter.Type.Header -> 1
                // retrofit2 requires query parameters after path parameters
                SpecParameter.Type.Path -> 2
                SpecParameter.Type.Query -> 3
            }
        private val preferredMimes: List<String> = listOf(
            Constants.MIME_TYPE_JSON,
            Constants.MIME_TYPE_MULTIPART_FORM_DATA,
            Constants.MIME_TYPE_URL_ENCODED
        )
    }
}

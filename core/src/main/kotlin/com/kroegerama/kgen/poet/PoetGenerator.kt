package com.kroegerama.kgen.poet

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.spec.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.time.format.DateTimeFormatter

class PoetGenerator(
    private val specModel: SpecModel,
    private val options: OptionSet
) {
    private val types = PoetTypes(options)

    fun createFileSpecs(): List<FileSpec> {
        val apiFile = createApiFile()
        val modelFile = createModelFile()
        val servicesFile = createServicesFile()

        return listOf(
            apiFile,
            modelFile,
            servicesFile
        )
    }

    private fun createApiFile(): FileSpec {
        return poetFile(
            packageName = options.packageName,
            fileName = types.api.simpleName
        ) {
            addFileComment("%L", specModel.fileHeader)
            addType(createApi())
            addType(createAuth())
        }
    }

    private fun createModelFile(): FileSpec {
        return poetFile(
            packageName = options.modelPackage,
            fileName = "Models"
        ) {
            addFileComment("%L", specModel.fileHeader)
            val (types, typeAliases) = createTypes()
            addTypes(types)
            typeAliases.forEach(::addTypeAlias)
        }
    }

    private fun createServicesFile(): FileSpec {
        return poetFile(
            packageName = options.apiPackage,
            fileName = "Services"
        ) {
            addFileComment("%L", specModel.fileHeader)
            addTypes(createServices())
            addProperties(createServiceDelegates())
        }
    }

    private fun createApi(): TypeSpec {
        return poetObject(types.api) {
            val title = poetProperty("title", STRING, KModifier.CONST) {
                initializer("%S", specModel.metadata.title)
            }
            val description = poetProperty("description", STRING, KModifier.CONST) {
                initializer("%S", specModel.metadata.description.orEmpty())
            }
            val version = poetProperty("version", STRING, KModifier.CONST) {
                initializer("%S", specModel.metadata.version)
            }
            val createdAt = poetProperty("createdAt", STRING, KModifier.CONST) {
                initializer(
                    "%S",
                    specModel.metadata.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            }
            val servers = poetProperty("servers", PoetTypes.ListOfHttpUrl) {
                initializer(
                    buildCodeBlock {
                        addStatement("%M(", MemberName("kotlin.collections", "listOf"))
                        withIndent {
                            specModel.metadata.servers.forEach { server ->
                                addStatement("%S.%M(),", server, PoetMembers.ToHttpUrl)
                            }
                        }
                        addStatement(")")
                    }
                )
            }
            val holder = poetProperty("holder", PoetTypes.ApiHolder) {
                initializer(
                    "%T(servers.first())",
                    PoetTypes.ApiHolder
                )
            }
            val setAuthProvider = poetFunSpec("setAuthProvider") {
                val auth = poetParameter("auth", types.auth) { }
                addParameter(auth)
                addStatement("holder.authInterceptor.setAuthProvider(auth.id, auth::provideAuthItem)")
            }
            val clearAuthProvider = poetFunSpec("clearAuthProvider") {
                val auth = poetParameter("auth", types.auth) { }
                addParameter(auth)
                addStatement("holder.authInterceptor.clearAuthProvider(auth.id)")
            }

            addProperties(
                listOf(
                    title,
                    description,
                    version,
                    createdAt,
                    servers,
                    holder
                )
            )
            addFunctions(
                listOf(
                    setAuthProvider,
                    clearAuthProvider
                )
            )
        }
    }

    private fun createAuth(): TypeSpec {
        return poetInterface(types.auth) {
            addModifiers(KModifier.SEALED)
            val id = poetProperty("id", STRING) { }
            val provideAuthItem = poetFunSpec("provideAuthItem") {
                addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                returns(PoetTypes.AuthItem.nullable())
            }
            addProperty(id)
            addFunction(provideAuthItem)

            addTypes(createAuthItems())
        }
    }

    private fun createServices(): List<TypeSpec> {
        return specModel.apis.map { api ->
            createService(api)
        }
    }

    private fun createServiceDelegates(): List<PropertySpec> {
        return specModel.apis.map { api ->
            createServiceDelegate(api)
        }
    }

    private fun createService(api: SpecApi): TypeSpec {
        return poetInterface(types.apiServiceName(api.name)) {
            addAnnotation(jvmSuppressWildcards())
            addFunctions(api.operations.map(::createOperation))
        }
    }

    private fun createServiceDelegate(api: SpecApi): PropertySpec {
        val serviceName = types.apiServiceName(api.name)
        return poetProperty(api.name + "Api", serviceName) {
            getter(
                FunSpec.getterBuilder().apply {
                    addStatement(
                        "return %T.holder.getService<%T>()",
                        types.api,
                        serviceName
                    )
                }.build()
            )
        }
    }

    private fun createOperation(operation: SpecOperation): FunSpec {
        return poetFunSpec(operation.name) {
            addModifiers(KModifier.SUSPEND, KModifier.ABSTRACT)
            addAnnotation(
                http(
                    method = operation.method.name,
                    path = operation.path.trimStart('/'),
                    hasBody = operation.body != null
                )
            )
            if (operation.deprecated) {
                addAnnotation(deprecated())
            }
            if (operation.securityIds.isNotEmpty()) {
                addAnnotation(
                    authInterceptorToken(operation.securityIds)
                )
            }
            val response = convertSimpleType(operation.response.type)
            val either = PoetTypes.either(response)
            returns(
                returnType = either,
                kdoc = operation.response.description?.let { CodeBlock.of("%L", it) } ?: CodeBlock.builder().build()
            )
            operation.serverOverride?.let { override ->
                addParameter(
                    poetParameter(
                        "url",
                        PoetTypes.HttpUrl
                    ) {
                        addAnnotation(url())
                        defaultValue("%S.%M(),", override, PoetMembers.ToHttpUrl)
                    }
                )
            }
            addParameters(operation.parameters.map(::createParameter))
            if (operation.body != null) {
                val body = when (operation.type) {
                    SpecOperation.Type.Default -> createParameter(operation.body)
                    SpecOperation.Type.Multipart -> poetParameter("partMap", PoetTypes.PartMapType) {
                        addAnnotation(partMap())
                    }

                    SpecOperation.Type.UrlEncoded -> poetParameter("fieldMap", PoetTypes.FieldMapType) {
                        addAnnotation(fieldMap())
                    }

                    SpecOperation.Type.Unknown -> poetParameter("body", PoetTypes.RequestBody) {
                        addAnnotation(body())
                    }
                }
                addParameter(body)
            }
        }
    }

    private fun createParameter(parameter: SpecParameter): ParameterSpec {
        return poetParameter(parameter.name, convertSimpleType(parameter.schema).nullable(parameter.nullable)) {
            addAnnotation(parameter(parameter.type, parameter.rawName))
            if (parameter.nullable) {
                defaultValue("null")
            }
            parameter.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun createParameter(info: SpecOperation.SchemaInfo): ParameterSpec {
        return poetParameter("body", convertSimpleType(info.type).nullable(info.nullable)) {
            addAnnotation(body())
            if (info.nullable) {
                defaultValue("null")
            }
            info.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun createAuthItems(): List<TypeSpec> {
        return specModel.securitySchemes.map { scheme ->
            val className = types.auth.nestedClass(scheme.name)
            poetClass(className) {
                addModifiers(KModifier.DATA)
                addSuperinterface(types.auth)
                when (scheme.type) {
                    SpecSecurityScheme.Type.Basic -> {
                        val getBasic = poetProperty(
                            "getBasic",
                            LambdaTypeName.get(
                                returnType = PoetTypes.AuthItemBasic.nullable()
                            ).copy(
                                suspending = true
                            )
                        ) {}
                        val id = poetProperty("id", STRING) {
                            addModifiers(KModifier.OVERRIDE)
                            initializer("ID")
                        }
                        val provideAuthItem = poetFunSpec("provideAuthItem") {
                            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                            returns(PoetTypes.AuthItem.nullable())
                            addStatement("return getBasic()")
                        }
                        primaryConstructor(getBasic)
                        addProperty(id)
                        addFunction(provideAuthItem)
                    }

                    SpecSecurityScheme.Type.Bearer -> {
                        val getBearer = poetProperty(
                            "getBearer",
                            LambdaTypeName.get(
                                returnType = PoetTypes.AuthItemBearer.nullable()
                            ).copy(
                                suspending = true
                            )
                        ) {}
                        val id = poetProperty("id", STRING) {
                            addModifiers(KModifier.OVERRIDE)
                            initializer("ID")
                        }
                        val provideAuthItem = poetFunSpec("provideAuthItem") {
                            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                            returns(PoetTypes.AuthItem.nullable())
                            addStatement("return getBearer()")
                        }
                        primaryConstructor(getBearer)
                        addProperty(id)
                        addFunction(provideAuthItem)
                    }

                    SpecSecurityScheme.Type.Header,
                    SpecSecurityScheme.Type.Query,
                    SpecSecurityScheme.Type.Cookie -> {
                        val typePart = scheme.type.name
                        val getValueName = "get${typePart}Value"

                        val getValue = poetProperty(
                            getValueName,
                            LambdaTypeName.get(
                                returnType = STRING.nullable()
                            ).copy(
                                suspending = true
                            )
                        ) {}
                        val id = poetProperty("id", STRING) {
                            addModifiers(KModifier.OVERRIDE)
                            initializer("ID")
                        }
                        val provideAuthItem = poetFunSpec("provideAuthItem") {
                            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                            returns(PoetTypes.AuthItem.nullable())
                            addCode(
                                buildCodeBlock {
                                    beginControlFlow("return $getValueName()?.let {")
                                    addStatement("%T(", PoetTypes.AuthItemApiKey)
                                    withIndent {
                                        addStatement("position = %T.$typePart,", PoetTypes.AuthItemPosition)
                                        addStatement("name = %S,", scheme.propertyName)
                                        addStatement("value = it")
                                    }
                                    addStatement(")")
                                    endControlFlow()
                                }
                            )
                        }
                        primaryConstructor(getValue)
                        addProperty(id)
                        addFunction(provideAuthItem)
                    }
                }
                val companion = poetCompanionObject {
                    val id = poetProperty("ID", STRING) {
                        addModifiers(KModifier.CONST)
                        initializer("%S", scheme.rawName)
                    }
                    addProperty(id)
                }
                addType(companion)
            }
        }
    }

    private fun createTypes(): Pair<List<TypeSpec>, List<TypeAliasSpec>> {
        val typeAliases = mutableListOf<TypeAliasSpec>()

        fun inner(schemas: List<SpecSchema.NamedSpecSchema>): List<TypeSpec> {
            return schemas.mapNotNull { schema ->
                val name = types.modelName(*schema.typeNames.toTypedArray())
                when (schema) {
                    is SpecSchema.Enum -> poetEnum(name) {
                        addAnnotation(serializable())
                        if (schema.deprecated) {
                            addAnnotation(deprecated())
                        }
                        schema.description?.let {
                            addKdoc("%L", it)
                        }
                        if (options.useCompose) {
                            addAnnotation(immutable())
                        }
                        schema.constants.forEach { constant ->
                            addEnumConstant(
                                name = constant.name,
                                typeSpec = poetAnonymousClass {
                                    addAnnotation(serialName(constant.value))
                                }
                            )
                        }
                    }

                    is SpecSchema.Object -> poetClass(name) {
                        addModifiers(KModifier.DATA)
                        addAnnotation(serializable())
                        if (schema.deprecated) {
                            addAnnotation(deprecated())
                        }
                        schema.description?.let {
                            addKdoc("%L", it)
                        }
                        if (options.useCompose) {
                            addAnnotation(immutable())
                        }
                        specModel.modelSerialNames[schema.typeNames]?.let { serialName ->
                            addAnnotation(serialName(serialName))
                        }
                        val properties: Array<PropertySpec> = schema.properties.map(::convertSpecProperty).toTypedArray()
                        primaryConstructor(*properties)
                        addTypes(inner(schema.children))

                        specModel.modelInterfaces[schema.typeNames]?.let { interfaces ->
                            interfaces.forEach {
                                addSuperinterface(types.modelName(*it.toTypedArray()))
                            }
                        }
                    }

                    is SpecSchema.Sealed -> poetInterface(name) {
                        addModifiers(KModifier.SEALED)
                        addAnnotation(serializable())
                        if (schema.deprecated) {
                            addAnnotation(deprecated())
                        }
                        schema.description?.let {
                            addKdoc("%L", it)
                        }
                        if (options.useCompose) {
                            addAnnotation(immutable())
                        }
                        addAnnotation(discriminator(schema.discriminator))
                        addTypes(inner(schema.children))
                    }

                    is SpecSchema.Typealias -> {
                        typeAliases += poetTypeAlias(
                            name = schema.typeNames.joinToString(""),
                            typeName = convertSimpleType(schema.schema)
                        ) {
                            if (schema.deprecated) {
                                addAnnotation(deprecated())
                            }
                            schema.description?.let {
                                addKdoc("%L", it)
                            }
                        }

                        null
                    }
                }
            }
        }

        val types = inner(specModel.schemas)
        return types to typeAliases
    }

    private fun convertSpecProperty(property: SpecProperty): PropertySpec {
        val typeName = convertSimpleType(property.type).nullable(
            nullable = property.nullable
        )
        return poetProperty(property.name, typeName) {
            addAnnotation(serialName(property.rawName))
            if (property.deprecated) {
                addAnnotation(deprecated())
            }
            property.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun convertSimpleType(simpleType: SpecSchema.SimpleType): TypeName = when (simpleType) {
        is SpecSchema.Primitive -> when (simpleType.type) {
            SpecPrimitiveType.Boolean -> BOOLEAN
            SpecPrimitiveType.Int32 -> INT
            SpecPrimitiveType.Int64 -> LONG
            SpecPrimitiveType.Float -> FLOAT
            SpecPrimitiveType.Double -> DOUBLE
            SpecPrimitiveType.String -> STRING
            SpecPrimitiveType.Date -> PoetTypes.SerializableLocalDate
            SpecPrimitiveType.Time -> PoetTypes.SerializableLocalTime
            SpecPrimitiveType.DateTime -> PoetTypes.SerializableOffsetDateTime
            SpecPrimitiveType.Base64 -> PoetTypes.SerializableBase64
            SpecPrimitiveType.UUID -> PoetTypes.SerializableUUID
        }

        is SpecSchema.Array -> LIST.parameterizedBy(
            convertSimpleType(simpleType.items).nullable(simpleType.itemsNullable)
        )

        is SpecSchema.Map -> MAP.parameterizedBy(
            STRING,
            convertSimpleType(simpleType.items).nullable(simpleType.itemsNullable)
        )

        is SpecSchema.Ref -> types.modelName(*simpleType.typeNames.toTypedArray())
        is SpecSchema.AnyComplex -> PoetTypes.JsonElement
        SpecSchema.Raw -> PoetTypes.ResponseBody
        SpecSchema.Unit -> UNIT
    }

    private fun url() = poetAnnotation(PoetTypes.Url) {}

    private fun serializable() = poetAnnotation(PoetTypes.Serializable) {}

    private fun discriminator(discriminator: String) = poetAnnotation(PoetTypes.JsonClassDiscriminator) {
        addMember("%S", discriminator)
    }

    private fun serialName(name: String) = poetAnnotation(PoetTypes.SerialName) {
        addMember("%S", name)
    }

    private fun deprecated() = poetAnnotation(PoetTypes.Deprecated) {
        addMember("%S", "Deprecated via OpenAPI Spec")
    }

    private fun immutable() = poetAnnotation(PoetTypes.Immutable) {}

    private fun jvmSuppressWildcards() = poetAnnotation(PoetTypes.JvmSuppressWildcards) {}

    private fun http(
        method: String,
        path: String?,
        hasBody: Boolean
    ) = poetAnnotation(PoetTypes.HTTP) {
        addMember("method = %S", method)
        if (path != null) {
            addMember("path = %S", path)
        }
        addMember("hasBody = %L", hasBody)
    }

    private fun authInterceptorToken(securityIds: List<String>) = poetAnnotation(PoetTypes.AuthInterceptorToken) {
        addMember(
            CodeBlock.builder().apply {
                securityIds.forEachIndexed { index, securityId ->
                    if (index > 0) {
                        add(", ")
                    }
                    add("%T.ID", types.auth.nestedClass(securityId))
                }
            }.build()
        )
    }

    private fun parameter(type: SpecParameter.Type, rawName: String) = poetAnnotation(
        when (type) {
            SpecParameter.Type.Cookie -> TODO()
            SpecParameter.Type.Header -> PoetTypes.Header
            SpecParameter.Type.Path -> PoetTypes.Path
            SpecParameter.Type.Query -> PoetTypes.Query
        }
    ) {
        addMember("%S", rawName)
    }

    private fun body() = poetAnnotation(PoetTypes.Body) {}

    private fun partMap() = poetAnnotation(PoetTypes.PartMap) {}

    private fun fieldMap() = poetAnnotation(PoetTypes.FieldMap) {}
}

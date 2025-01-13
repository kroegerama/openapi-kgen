package com.kroegerama.kgen.poet

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.Constants.AUTH_HEADER_VALUE_NAME_PREFIX
import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.Util
import com.kroegerama.kgen.asBaseUrl
import com.kroegerama.kgen.language.asConstantName
import com.kroegerama.kgen.language.asFunctionName
import com.kroegerama.kgen.openapi.OpenAPIAnalyzer
import com.kroegerama.kgen.openapi.SecurityType
import com.kroegerama.kgen.openapi.mapToType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityScheme
import java.lang.reflect.Type

interface IBaseFilesGenerator {
    fun getBaseFiles(): List<FileSpec>
}

class BaseFilesGenerator(
    openAPI: OpenAPI,
    options: OptionSet,
    analyzer: OpenAPIAnalyzer
) : IBaseFilesGenerator,
    IPoetGeneratorBase by PoetGeneratorBase(openAPI, options, analyzer) {

    override fun getBaseFiles() = listOf(
        getMetadataFile(),
        getApiHolderFile(),
        getAuthInterceptorFile(),
        getEnumConverterFile(),
        getDateConverterFactoryFile(),
        getDateJsonAdapters()
    )

    private fun getMetadataFile() = prepareFileSpec(options.packageName, "Metadata") {
        val tBuildConfig = poetObject(ClassName(options.packageName, "ApiBuildConfig")) {
            val info = openAPI.info
            addProperty(
                poetProperty("API_VERSION", STRING, KModifier.CONST) {
                    addKdoc("Value of **OpenAPI.info.version**")
                    initializer("%S", info.version)
                }
            )
            addProperty(
                poetProperty("API_TITLE", STRING, KModifier.CONST) {
                    addKdoc("Value of **OpenAPI.info.title**")
                    initializer("%S", info.title)
                }
            )
            addProperty(
                poetProperty("API_DESCRIPTION", STRING.nullable(true)) {
                    addKdoc("Value of **OpenAPI.info.description**")
                    initializer("%S", info.description)
                }
            )
            addProperty(
                poetProperty("GEN_FORMATTED", STRING, KModifier.CONST) {
                    addKdoc("Time of code generation. Formatted as **RFC 1123** date time.")
                    initializer("%S", Util.formattedDateTime)
                }
            )
            addProperty(
                poetProperty("GEN_TIMESTAMP", LONG, KModifier.CONST) {
                    addKdoc("Time of code generation. **Unix timestamp** in seconds since 1970-01-01T00:00:00Z.")
                    initializer("%L", Util.unixDateTime)
                }
            )
        }

        val serverList = poetProperty("serverList", LIST.parameterizedBy(STRING)) {
            val servers = openAPI.servers
            val pathParamRegex = """[{](\S+?)[}]""".toRegex()

            val block = CodeBlock.builder().apply {
                servers.forEachIndexed { index, server ->
                    val resolvedUrl = server.url.replace(pathParamRegex) { r ->
                        val key = r.groupValues[1]
                        server.variables[key]?.run {
                            default ?: enum?.firstOrNull()
                        } ?: r.value
                    }
                    val baseUrl = resolvedUrl.asBaseUrl()
                    add("%S", baseUrl)
                    if (index < servers.size - 1) add(", ")
                }
            }.build()

            initializer("%M(%L)", PoetConstants.LIST_OF, block)
        }

        addType(tBuildConfig)
        addProperty(serverList)
    }

    private fun getApiHolderFile() = prepareFileSpec(options.packageName, "ApiHolder") {
        val cnApiDecorator = ClassName(options.packageName, "ApiDecorator")
        val cnApiHolder = ClassName(options.packageName, "ApiHolder")
        val cnInterceptor = ClassName(options.packageName, "ApiAuthInterceptor")

        val fnInvalidate = MemberName(cnApiHolder, "invalidate")
        val fnDecorate = MemberName(cnApiHolder, "decorate")
        val fnCreateMoshi = MemberName(cnApiHolder, "createMoshi")
        val fnCreateClient = MemberName(cnApiHolder, "createClient")
        val fnCreateRetrofit = MemberName(cnApiHolder, "createRetrofit")
        val fnGetApi = MemberName(cnApiHolder, "getApi")

        val mnDecorator = MemberName(cnApiHolder, "decorator")
        val mnClient = MemberName(cnApiHolder, "client")
        val mnCurrentClient = MemberName(cnApiHolder, "currentClient")
        val mnRetrofit = MemberName(cnApiHolder, "retrofit")
        val mnCurrentRetrofit = MemberName(cnApiHolder, "currentRetrofit")
        val mnMoshi = MemberName(cnApiHolder, "moshi")
        val mnCurrentMoshi = MemberName(cnApiHolder, "currentMoshi")

        val mnApiHolder = MemberName(cnApiHolder, "apiHolder")

        val tDecorator = poetInterface(cnApiDecorator) {
            val moshiDecorator = poetFunSpec(fnDecorate.simpleName) {
                receiver(PoetConstants.MOSHI_BUILDER)
                addStatement("return %T", UNIT)
            }
            val clientDecorator = poetFunSpec(fnDecorate.simpleName) {
                receiver(PoetConstants.OK_CLIENT_BUILDER)
                addStatement("return %T", UNIT)
            }
            val retrofitDecorator = poetFunSpec(fnDecorate.simpleName) {
                receiver(PoetConstants.RETROFIT_BUILDER)
                addStatement("return %T", UNIT)
            }
            addFunction(moshiDecorator)
            addFunction(clientDecorator)
            addFunction(retrofitDecorator)
        }
        val tApiHolder = poetObject(cnApiHolder) {
            val pDecorator = poetProperty(mnDecorator.simpleName, cnApiDecorator.nullable(true)) {
                mutable()
                initializer("null")

                val setFun = FunSpec.setterBuilder().apply {
                    addParameter("value", cnApiDecorator)
                    addStatement("field = value")
                    addStatement("%N()", fnInvalidate)
                }.build()
                setter(setFun)
            }

            val pCurrentMoshi = poetProperty(mnCurrentMoshi.simpleName, PoetConstants.MOSHI.nullable(true)) {
                mutable()
                addModifiers(KModifier.PRIVATE)
                initializer("null")
            }

            val pCurrentClient = poetProperty(mnCurrentClient.simpleName, PoetConstants.OK_CLIENT.nullable(true)) {
                mutable()
                addModifiers(KModifier.PRIVATE)
                initializer("null")
            }

            val pCurrentRetrofit = poetProperty(mnCurrentRetrofit.simpleName, PoetConstants.RETROFIT.nullable(true)) {
                mutable()
                addModifiers(KModifier.PRIVATE)
                initializer("null")
            }

            val pMoshi = poetProperty(mnMoshi.simpleName, PoetConstants.MOSHI) {
                val getFun = FunSpec.getterBuilder().apply {
                    addStatement("return %N ?: %N().also { %N = it }", mnCurrentMoshi, fnCreateMoshi, mnCurrentMoshi)
                }.build()
                getter(getFun)
            }

            val pClient = poetProperty(mnClient.simpleName, PoetConstants.OK_CLIENT) {
                val getFun = FunSpec.getterBuilder().apply {
                    addStatement("return %N ?: %N().also { %N = it }", mnCurrentClient, fnCreateClient, mnCurrentClient)
                }.build()
                getter(getFun)
            }

            val pRetrofit = poetProperty(mnRetrofit.simpleName, PoetConstants.RETROFIT) {
                val getFun = FunSpec.getterBuilder().apply {
                    addStatement(
                        "return %N ?: %N().also { %N = it }",
                        mnCurrentRetrofit,
                        fnCreateRetrofit,
                        mnCurrentRetrofit
                    )
                }.build()
                getter(getFun)
            }

            val pApiMap = poetProperty(
                mnApiHolder.simpleName,
                MUTABLE_MAP.parameterizedBy(Class::class.asClassName().parameterizedBy(STAR), ANY)
            ) {
                addModifiers(KModifier.PRIVATE)
                initializer("%M()", PoetConstants.MUTABLE_MAP_OF)
            }

            val fInvalidate = poetFunSpec(fnInvalidate.simpleName) {
                addStatement("%N = null", mnCurrentMoshi)
                addStatement("%N = null", mnCurrentClient)
                addStatement("%N = null", mnCurrentRetrofit)
                addStatement("%N.clear()", mnApiHolder)
            }

            val fCreateMoshi = poetFunSpec(fnCreateMoshi.simpleName) {
                addModifiers(KModifier.PRIVATE)
                beginControlFlow("return %T().run", PoetConstants.MOSHI_BUILDER)
                addStatement("add(%T::class.java, %T())", PoetConstants.DATE, PoetConstants.RFC_DATE_ADAPTER)
                addStatement(
                    "add(%T::class.java, %T)",
                    PoetConstants.LOCAL_DATE,
                    ClassName(options.packageName, "LocalDateJsonAdapter")
                )
                addStatement(
                    "add(%T::class.java, %T)",
                    PoetConstants.OFFSET_DATE_TIME,
                    ClassName(options.packageName, "OffsetDateTimeJsonAdapter")
                )
                addStatement("%N?.apply { decorate() }", mnDecorator)
                addStatement("build()")
                endControlFlow()
                returns(PoetConstants.MOSHI)
            }

            val fCreateClient = poetFunSpec(fnCreateClient.simpleName) {
                addModifiers(KModifier.PRIVATE)
                beginControlFlow("return %T().run", PoetConstants.OK_CLIENT_BUILDER)
                addStatement("addInterceptor(%T)", cnInterceptor)
                addStatement("%N?.apply { decorate() }", mnDecorator)
                addStatement("build()")
                endControlFlow()
                returns(PoetConstants.OK_CLIENT)
            }

            val fCreateRetrofit = poetFunSpec(fnCreateRetrofit.simpleName) {
                addModifiers(KModifier.PRIVATE)
                beginControlFlow("return %T().run", PoetConstants.RETROFIT_BUILDER)
                addStatement("baseUrl(%M.first())", MemberName(options.packageName, "serverList"))
                addStatement("client(%N)", mnClient)
                addStatement(
                    "addConverterFactory(%T.create())",
                    ClassName("retrofit2.converter.scalars", "ScalarsConverterFactory")
                )
                addStatement(
                    "addConverterFactory(%T.create(%N))",
                    ClassName("retrofit2.converter.moshi", "MoshiConverterFactory"),
                    mnMoshi
                )
                addStatement("addConverterFactory(%T)", ClassName(options.packageName, "EnumConverterFactory"))
                addStatement("addConverterFactory(%T)", ClassName(options.packageName, "DateConverterFactory"))
                addStatement("%N?.apply { decorate() }", mnDecorator)
                addStatement("build()")
                endControlFlow()
                returns(PoetConstants.RETROFIT)
            }

            val fGetApi = poetFunSpec(fnGetApi.simpleName) {
                addModifiers(KModifier.INLINE, KModifier.INTERNAL)
                val generic = TypeVariableName("T").copy(reified = true, bounds = listOf(ANY))
                returns(generic)
                addTypeVariable(generic)

                addCode(
                    CodeBlock.builder()
                        .add("return %N.getOrPut(%T::class.java) {\n", mnApiHolder, generic)
                        .indent()
                        .add("%N.%M<%T>()\n", mnRetrofit, PoetConstants.RETROFIT_CREATE_FUN, generic)
                        .unindent()
                        .add("} as %T\n", generic)
                        .build()
                )
            }

            addProperty(pDecorator)
            addProperty(pCurrentMoshi)
            addProperty(pCurrentClient)
            addProperty(pCurrentRetrofit)
            addProperty(pMoshi)
            addProperty(pClient)
            addProperty(pRetrofit)
            addProperty(pApiMap)
            addFunction(fInvalidate)
            addFunction(fCreateMoshi)
            addFunction(fCreateClient)
            addFunction(fCreateRetrofit)
            addFunction(fGetApi)
        }

        addType(tDecorator)
        addType(tApiHolder)
    }

    private fun getAuthInterceptorFile() = prepareFileSpec(options.packageName, "ApiAuthInterceptor") {
        val cnInterceptor = ClassName(options.packageName, "ApiAuthInterceptor")
        val cnAuthInfo = cnInterceptor.nestedClass("AuthInfo")
        val cnAuthPosition = cnInterceptor.nestedClass("AuthPosition")

        val mnAuthPositionHeader = MemberName(cnAuthPosition, "Header")
        val mnAuthPositionQuery = MemberName(cnAuthPosition, "Query")

        val tAuthInfo = createAuthInfoClass(cnAuthInfo, cnAuthPosition, mnAuthPositionHeader, mnAuthPositionQuery)
        val tAuthPosition = poetEnum(cnAuthPosition) {
            addEnumConstant(mnAuthPositionHeader.simpleName)
            addEnumConstant(mnAuthPositionQuery.simpleName)
        }
        val tInterceptor = createInterceptorClass(
            cnInterceptor,
            cnAuthInfo,
            mnAuthPositionHeader,
            mnAuthPositionQuery,
            tAuthInfo,
            tAuthPosition
        )

        addType(tInterceptor)
    }

    private fun createInterceptorClass(
        cnInterceptor: ClassName,
        cnAuthInfo: ClassName,
        mnAuthPositionHeader: MemberName,
        mnAuthPositionQuery: MemberName,
        tAuthInfo: TypeSpec,
        tAuthPosition: TypeSpec
    ) = poetObject(cnInterceptor) {
        addSuperinterface(PoetConstants.OK_INTERCEPTOR)

        val mnAuthProviderMap = MemberName(cnInterceptor, "authProviderMap")
        val authGenLambda = LambdaTypeName.get(
            returnType = cnAuthInfo.nullable(true)
        )

        val pAuthGenMap = poetProperty(
            mnAuthProviderMap.simpleName,
            MUTABLE_MAP.parameterizedBy(STRING, authGenLambda),
            KModifier.PRIVATE
        ) {
            initializer("%M()", PoetConstants.MUTABLE_MAP_OF)
        }
        val fClearAll = poetFunSpec("clearAllAuth") {
            addStatement("return %N.clear()", mnAuthProviderMap)
        }

        val securityHeaderValues = mutableListOf<PropertySpec>()
        val securityFuns = analyzer.securitySchemes.map { (securityName, securityScheme) ->
            val headerValueProperty = poetProperty("$AUTH_HEADER_VALUE_NAME_PREFIX$securityName".asConstantName(), STRING, KModifier.CONST) {
                initializer("%S", securityName)
            }
            securityHeaderValues += headerValueProperty
            createSecurityFuns(cnAuthInfo, mnAuthProviderMap, securityName, headerValueProperty, securityScheme)
        }.flatten()

        val headerName = poetProperty(Constants.AUTH_HEADER_NAME, STRING, KModifier.CONST) {
            initializer("%S", Constants.AUTH_HEADER_VALUE)
        }

        val fIntercept = poetFunSpec("intercept") {
            addModifiers(KModifier.OVERRIDE)
            addParameter("chain", PoetConstants.OK_INTERCEPTOR_CHAIN)
            returns(PoetConstants.OK_RESPONSE)
            addCode(
                """
                val request = chain.request()
                val authHeaders = request.headers(AUTH_INFO_HEADER)
                if (authHeaders.isEmpty()) {
                    return chain.proceed(request)
                }

                val newRequest = request.newBuilder().run {
                    removeHeader(AUTH_INFO_HEADER)
                    val required = authProviderMap.filter { it.key in authHeaders }.values
                    required.forEach { provider ->
                        val authInfo = provider() ?: return@forEach
                        when (authInfo.position) {
                            %M -> addHeader(authInfo.paramName, authInfo.paramValue)
                            %M -> url(
                                request.url.newBuilder()
                                    .addQueryParameter(authInfo.paramName, authInfo.paramValue)
                                    .build()
                            )
                        }
                    }
                    build()
                }
                return chain.proceed(newRequest)
            """.trimIndent(),
                mnAuthPositionHeader,
                mnAuthPositionQuery
            )
        }

        addProperty(pAuthGenMap)
        addProperty(headerName)
        addProperties(securityHeaderValues)
        addFunctions(securityFuns)
        addFunction(fClearAll)
        addFunction(fIntercept)
        addType(tAuthInfo)
        addType(tAuthPosition)
    }

    private fun createAuthInfoClass(
        cnAuthInfo: ClassName,
        cnAuthPosition: ClassName,
        mnAuthPositionHeader: MemberName,
        mnAuthPositionQuery: MemberName
    ) = poetClass(cnAuthInfo) {
        addModifiers(KModifier.SEALED)
        val pPosition = poetProperty("position", cnAuthPosition) {}
        val pParamName = poetProperty("paramName", STRING) {}
        val pParamValue = poetProperty("paramValue", STRING) {}
        primaryConstructor(pPosition, pParamName, pParamValue)

        val cBasic = poetClass(cnAuthInfo.nestedClass("Basic")) {
            primaryConstructor(
                poetProperty("username", STRING) {},
                poetProperty("password", STRING) {}
            )
            superclass(cnAuthInfo)
            addSuperclassConstructorParameter("%M", mnAuthPositionHeader)
            addSuperclassConstructorParameter("%S", "Authorization")
            addSuperclassConstructorParameter("%T.basic(%N, %N)", PoetConstants.OK_CREDENTIALS, "username", "password")
        }
        val cBearer = poetClass(cnAuthInfo.nestedClass("Bearer")) {
            primaryConstructor(
                poetProperty("token", STRING) {}
            )
            superclass(cnAuthInfo)
            addSuperclassConstructorParameter("%M", mnAuthPositionHeader)
            addSuperclassConstructorParameter("%S", "Authorization")
            addSuperclassConstructorParameter("%P", "Bearer \$token")
        }
        val cHeader = poetClass(cnAuthInfo.nestedClass("Header")) {
            primaryConstructor(
                poetProperty("name", STRING) {},
                poetProperty("value", STRING) {}
            )
            superclass(cnAuthInfo)
            addSuperclassConstructorParameter("%M", mnAuthPositionHeader)
            addSuperclassConstructorParameter("%N", "name")
            addSuperclassConstructorParameter("%N", "value")
        }
        val cQuery = poetClass(cnAuthInfo.nestedClass("Query")) {
            primaryConstructor(
                poetProperty("name", STRING) {},
                poetProperty("value", STRING) {}
            )
            superclass(cnAuthInfo)
            addSuperclassConstructorParameter("%M", mnAuthPositionQuery)
            addSuperclassConstructorParameter("%N", "name")
            addSuperclassConstructorParameter("%N", "value")
        }
        val cOAuth = poetClass(cnAuthInfo.nestedClass("OAuth")) {
            primaryConstructor(
                poetProperty("oauth", STRING) {}
            )
            superclass(cnAuthInfo)
            addSuperclassConstructorParameter("%M", mnAuthPositionHeader)
            addSuperclassConstructorParameter("%S", "Authorization")
            addSuperclassConstructorParameter("%N", "oauth")
        }

        addType(cBasic)
        addType(cBearer)
        addType(cHeader)
        addType(cQuery)
        addType(cOAuth)
    }

    private fun createSecurityFuns(
        cnAuthInfo: ClassName,
        mnAuthMap: MemberName,
        name: String,
        headerValueProperty: PropertySpec,
        scheme: SecurityScheme
    ): List<FunSpec> {
        val fnSetProvider = "set $name provider".asFunctionName()
        val fnSet = "set $name".asFunctionName()
        val fnClear = "clear $name".asFunctionName()

        val fSetProvider = poetFunSpec(fnSetProvider) {
            val providerType = when (scheme.mapToType()) {
                SecurityType.Basic -> LambdaTypeName.get(
                    returnType = cnAuthInfo.nestedClass("Basic").nullable(true)
                )

                SecurityType.Bearer -> LambdaTypeName.get(
                    returnType = cnAuthInfo.nestedClass("Bearer").nullable(true)
                )

                SecurityType.Header -> LambdaTypeName.get(
                    returnType = cnAuthInfo.nestedClass("Header").nullable(true)
                )

                SecurityType.Query -> LambdaTypeName.get(
                    returnType = cnAuthInfo.nestedClass("Query").nullable(true)
                )

                SecurityType.OAuth -> LambdaTypeName.get(
                    returnType = cnAuthInfo.nestedClass("OAuth").nullable(true)
                )

                SecurityType.Unknown -> throw IllegalStateException("SecurityScheme not supported: $scheme")
            }
            addParameter(
                "provider",
                providerType
            )
            addStatement("%N[%N] = %N", mnAuthMap, headerValueProperty, "provider")
        }

        val fSet = poetFunSpec(fnSet) {
            when (scheme.mapToType()) {
                SecurityType.Basic -> {
                    addParameter("username", STRING)
                    addParameter("password", STRING)
                    addStatement("%N { %T(username, password) }", fSetProvider, cnAuthInfo.nestedClass("Basic"))
                }

                SecurityType.Bearer -> {
                    addParameter("token", STRING)
                    addStatement("%N { %T(token) }", fSetProvider, cnAuthInfo.nestedClass("Bearer"))
                }

                SecurityType.Header -> {
                    addParameter("apiKey", STRING)
                    addStatement("%N { %T(%S, apiKey) }", fSetProvider, cnAuthInfo.nestedClass("Header"), scheme.name)
                }

                SecurityType.Query -> {
                    addParameter("apiKey", STRING)
                    addStatement("%N { %T(%S, apiKey) }", fSetProvider, cnAuthInfo.nestedClass("Query"), scheme.name)
                }

                SecurityType.OAuth -> {
                    addParameter("oauth", STRING)
                    addStatement("%N { %T(oauth) }", fSetProvider, cnAuthInfo.nestedClass("OAuth"))
                }

                SecurityType.Unknown -> {
                    throw IllegalStateException("SecurityScheme not supported: $scheme")
                }
            }
        }

        val fClear = poetFunSpec(fnClear) {
            addStatement("%N.remove(%N)", mnAuthMap, headerValueProperty)
        }

        return listOf(fSetProvider, fSet, fClear)

//        val fSet = poetFunSpec(fnSet) {
//            when (scheme.mapToType()) {
//                SecurityType.Basic -> {
//                    addParameter("username", STRING)
//                    addParameter("password", STRING)
//                    addStatement("%N[%S] = %T.basic(username, password)", mnAuthMap, name, cnAuthInfo)
//                }
//
//                SecurityType.Bearer -> {
//                    addParameter("token", STRING)
//                    addStatement("%N[%S] = %T.bearer(token)", mnAuthMap, name, cnAuthInfo)
//                }
//
//                SecurityType.Header -> {
//                    addParameter("apiKey", STRING)
//                    addStatement("%N[%S] = %T.header(%S, apiKey)", mnAuthMap, name, cnAuthInfo, scheme.name)
//                }
//
//                SecurityType.Query -> {
//                    addParameter("apiKey", STRING)
//                    addStatement("%N[%S] = %T.query(%S, apiKey)", mnAuthMap, name, cnAuthInfo, scheme.name)
//                }
//
//                SecurityType.OAuth -> {
//                    addParameter("oauth", STRING)
//                    addStatement("%N[%S] = %T.header(%S, oauth)", mnAuthMap, name, cnAuthInfo, "Authorization")
//                }
//
//                SecurityType.Unknown -> {
//                    throw IllegalStateException("SecurityScheme not supported: $scheme")
//                }
//            }
//        }
//
//        val fClear = poetFunSpec(fnClear) {
//            addStatement("%N.remove(%S)", mnAuthMap, name)
//        }
//
//        return listOf(fSet, fClear)
    }

    private fun getEnumConverterFile() = prepareFileSpec(options.packageName, "EnumConverterFactory") {
        val name = ClassName(options.packageName, "EnumConverterFactory")
        val fnStringConverter = MemberName(name, "stringConverter")
        val fnCreateEnumConverter = MemberName(name, "createEnumConverter")

        val cStar = Class::class.asClassName().parameterizedBy(STAR)

        val enumConverterFactory = poetObject(name) {
            superclass(PoetConstants.CONVERTER_FACTORY)

            val stringConverterFun = poetFunSpec(fnStringConverter.simpleName) {
                addModifiers(KModifier.OVERRIDE)
                addParameter("type", Type::class)
                addParameter("annotations", ARRAY.parameterizedBy(ANNOTATION))
                addParameter("retrofit", PoetConstants.RETROFIT)
                addStatement("return if (type is %T && type.isEnum) %N() else null", cStar, fnCreateEnumConverter)
                returns(PoetConstants.CONVERTER.parameterizedBy(ENUM.parameterizedBy(STAR), STRING).nullable(true))
            }
            val createEnumConverterFun = poetFunSpec(fnCreateEnumConverter.simpleName) {
                addModifiers(KModifier.PRIVATE)
                returns(PoetConstants.CONVERTER.parameterizedBy(ENUM.parameterizedBy(STAR), STRING))
                addStatement(
                    """
                    return %T { enum ->
                        try {
                            enum.javaClass.getField(enum.name).getAnnotation(%T::class.java)?.name
                        } catch (e: %T) {
                            null
                        } ?: enum.toString()
                    }""".trimIndent(),
                    PoetConstants.CONVERTER,
                    PoetConstants.MOSHI_JSON,
                    PoetConstants.EXCEPTION
                )
            }

            addFunction(stringConverterFun)
            addFunction(createEnumConverterFun)
        }
        addType(enumConverterFactory)
    }

    private fun getDateConverterFactoryFile() = prepareFileSpec(options.packageName, "DateConverterFactory") {
        val name = ClassName(options.packageName, "DateConverterFactory")
        val fnStringConverter = MemberName(name, "stringConverter")
        val fnCreateLocalDateConverter = MemberName(name, "createLocalDateConverter")
        val fnCreateOffsetDateTimeConverter = MemberName(name, "createOffsetDateTimeConverter")

        val factory = poetObject(name) {
            superclass(PoetConstants.CONVERTER_FACTORY)

            val stringConverterFun = poetFunSpec(fnStringConverter.simpleName) {
                addModifiers(KModifier.OVERRIDE)
                addParameter("type", Type::class)
                addParameter("annotations", ARRAY.parameterizedBy(ANNOTATION))
                addParameter("retrofit", PoetConstants.RETROFIT)
                returns(PoetConstants.CONVERTER.parameterizedBy(STAR, STRING).nullable(true))
                addStatement(
                    """
                    return when (type) {
                        %T::class.java -> createLocalDateConverter()
                        %T::class.java -> createOffsetDateTimeConverter()
                        else -> null
                    }""".trimIndent(),
                    PoetConstants.LOCAL_DATE,
                    PoetConstants.OFFSET_DATE_TIME,
                )
            }
            val localDateConverterFun = poetFunSpec(fnCreateLocalDateConverter.simpleName) {
                addModifiers(KModifier.PRIVATE)
                returns(PoetConstants.CONVERTER.parameterizedBy(PoetConstants.LOCAL_DATE, STRING))
                addStatement("return %T(%T::toString)", PoetConstants.CONVERTER, PoetConstants.LOCAL_DATE)
            }
            val offsetDateTimeConverterFun = poetFunSpec(fnCreateOffsetDateTimeConverter.simpleName) {
                addModifiers(KModifier.PRIVATE)
                returns(PoetConstants.CONVERTER.parameterizedBy(PoetConstants.OFFSET_DATE_TIME, STRING))
                addStatement("return %T(%T::toString)", PoetConstants.CONVERTER, PoetConstants.OFFSET_DATE_TIME)
            }

            addFunction(stringConverterFun)
            addFunction(localDateConverterFun)
            addFunction(offsetDateTimeConverterFun)
        }
        addType(factory)
    }

    private fun getDateJsonAdapters() = prepareFileSpec(options.packageName, "DateJsonAdapters") {
        val localDateConverter = poetObject(ClassName(options.packageName, "LocalDateJsonAdapter")) {
            superclass(PoetConstants.MOSHI_JSON_ADAPTER.parameterizedBy(PoetConstants.LOCAL_DATE))

            val fromJson = poetFunSpec("fromJson") {
                addModifiers(KModifier.OVERRIDE)
                addParameter("reader", PoetConstants.JSON_READER)
                returns(PoetConstants.LOCAL_DATE.nullable(true))
                addStatement(
                    """
                    return if (reader.peek() == %T.Token.NULL) {
                        reader.nextNull()
                    } else {
                        %T.parse(reader.nextString())
                    }""".trimIndent(),
                    PoetConstants.JSON_READER,
                    PoetConstants.LOCAL_DATE
                )
            }
            val toJson = poetFunSpec("toJson") {
                addModifiers(KModifier.OVERRIDE)
                addParameter("writer", PoetConstants.JSON_WRITER)
                addParameter("value", PoetConstants.LOCAL_DATE.nullable(true))
                addStatement(
                    """
                    if (value == null) {
                        writer.nullValue()
                    } else {
                        writer.value(value.toString())
                    }""".trimIndent()
                )
            }

            addFunction(fromJson)
            addFunction(toJson)
        }
        val offsetDateTimeConverter = poetObject(ClassName(options.packageName, "OffsetDateTimeJsonAdapter")) {
            superclass(PoetConstants.MOSHI_JSON_ADAPTER.parameterizedBy(PoetConstants.OFFSET_DATE_TIME))

            val fromJson = poetFunSpec("fromJson") {
                addModifiers(KModifier.OVERRIDE)
                addParameter("reader", PoetConstants.JSON_READER)
                returns(PoetConstants.OFFSET_DATE_TIME.nullable(true))
                addStatement(
                    """
                    return if (reader.peek() == %T.Token.NULL) {
                        reader.nextNull()
                    } else {
                        %T.parse(reader.nextString())
                    }""".trimIndent(),
                    PoetConstants.JSON_READER,
                    PoetConstants.OFFSET_DATE_TIME
                )
            }
            val toJson = poetFunSpec("toJson") {
                addModifiers(KModifier.OVERRIDE)
                addParameter("writer", PoetConstants.JSON_WRITER)
                addParameter("value", PoetConstants.OFFSET_DATE_TIME.nullable(true))
                addStatement(
                    """
                    if (value == null) {
                        writer.nullValue()
                    } else {
                        writer.value(value.toString())
                    }""".trimIndent()
                )
            }

            addFunction(fromJson)
            addFunction(toJson)
        }

        addType(localDateConverter)
        addType(offsetDateTimeConverter)
    }

}

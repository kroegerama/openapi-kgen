package com.kroegerama.kgen.poet

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.Util
import com.kroegerama.kgen.asBaseUrl
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
        getEnumConverterFile()
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

            val block = CodeBlock.builder().apply {
                servers.forEachIndexed { index, server ->
                    val baseUrl = server.url.asBaseUrl()
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
                    addStatement("return %N ?: %N().also { %N = it }", mnCurrentRetrofit, fnCreateRetrofit, mnCurrentRetrofit)
                }.build()
                getter(getFun)
            }

            val pApiMap = poetProperty(mnApiHolder.simpleName, MUTABLE_MAP.parameterizedBy(Class::class.asClassName().parameterizedBy(STAR), ANY)) {
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
                addStatement("add(%T::class.java, %T())", PoetConstants.DATE, PoetConstants.RFCDateAdapter)
                addStatement("%N?.apply { decorate() }", mnDecorator)
                addStatement("build()")
                endControlFlow()
            }

            val fCreateClient = poetFunSpec(fnCreateClient.simpleName) {
                addModifiers(KModifier.PRIVATE)
                beginControlFlow("return %T().run", PoetConstants.OK_CLIENT_BUILDER)
                addStatement("addInterceptor(%T)", cnInterceptor)
                addStatement("%N?.apply { decorate() }", mnDecorator)
                addStatement("build()")
                endControlFlow()
            }

            val fCreateRetrofit = poetFunSpec(fnCreateRetrofit.simpleName) {
                addModifiers(KModifier.PRIVATE)
                beginControlFlow("return %T().run", PoetConstants.RETROFIT_BUILDER)
                addStatement("baseUrl(%M.first())", MemberName(options.packageName, "serverList"))
                addStatement("client(%N)", mnClient)
                addStatement("addConverterFactory(%T.create())", ClassName("retrofit2.converter.scalars", "ScalarsConverterFactory"))
                addStatement("addConverterFactory(%T.create(%N))", ClassName("retrofit2.converter.moshi", "MoshiConverterFactory"), mnMoshi)
                addStatement("addConverterFactory(%T)", ClassName(options.packageName, "EnumConverterFactory"))
                addStatement("%N?.apply { decorate() }", mnDecorator)
                addStatement("build()")
                endControlFlow()
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
            addModifiers(KModifier.PRIVATE)

            addEnumConstant(mnAuthPositionHeader.simpleName)
            addEnumConstant(mnAuthPositionQuery.simpleName)
        }
        val tInterceptor = createInterceptorClass(cnInterceptor, cnAuthInfo, mnAuthPositionHeader, mnAuthPositionQuery, tAuthInfo, tAuthPosition)

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

        val mnAuthMap = MemberName(cnInterceptor, "authMap")

        val pAuthMap = poetProperty(mnAuthMap.simpleName, MUTABLE_MAP.parameterizedBy(STRING, cnAuthInfo), KModifier.PRIVATE) {
            initializer("%M()", PoetConstants.MUTABLE_MAP_OF)
        }
        val fClearAll = poetFunSpec("clearAllAuth") {
            addStatement("return %N.clear()", mnAuthMap)
        }

        val securityFuns = analyzer.securitySchemes.map { (securityName, securityScheme) ->
            createSecurityFuns(cnAuthInfo, mnAuthMap, securityName, securityScheme)
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
                    val required = authMap.filter { it.key in authHeaders }.values
                    required.forEach { (position, paramName, paramValue) ->
                        when (position) {
                            %M -> addHeader(paramName, paramValue)
                            %M -> url(
                                request.url.newBuilder()
                                    .addQueryParameter(paramName, paramValue)
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

        addProperty(pAuthMap)
        addProperty(headerName)
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
        addModifiers(KModifier.PRIVATE, KModifier.DATA)
        val pPosition = poetProperty("position", cnAuthPosition) {}
        val pParamName = poetProperty("paramName", STRING) {}
        val pParamValue = poetProperty("paramValue", STRING) {}
        primaryConstructor(pPosition, pParamName, pParamValue)
        val companion = createCompanion(cnAuthInfo, mnAuthPositionHeader, mnAuthPositionQuery)
        addType(companion)
    }

    private fun createCompanion(
        cnAuthInfo: ClassName,
        mnAuthPositionHeader: MemberName,
        mnAuthPositionQuery: MemberName
    ) = TypeSpec.companionObjectBuilder().apply {
        val fBasic = poetFunSpec("basic") {
            addParameter("username", STRING)
            addParameter("password", STRING)
            addStatement(
                "return %T(%M, %S, %T.basic(username, password))",
                cnAuthInfo,
                mnAuthPositionHeader,
                "Authorization",
                PoetConstants.OK_CREDENTIALS
            )
        }

        val fBearer = poetFunSpec("bearer") {
            addParameter("token", STRING)
            addStatement(
                "return %T(%M, %S, %P)",
                cnAuthInfo,
                mnAuthPositionHeader,
                "Authorization",
                "Bearer \$token"
            )
        }

        val fHeader = poetFunSpec("header") {
            addParameter("name", STRING)
            addParameter("value", STRING)
            addStatement(
                "return %T(%M, name, value)",
                cnAuthInfo,
                mnAuthPositionHeader
            )
        }

        val fQuery = poetFunSpec("query") {
            addParameter("name", STRING)
            addParameter("value", STRING)
            addStatement(
                "return %T(%M, name, value)",
                cnAuthInfo,
                mnAuthPositionQuery
            )
        }

        addFunction(fBasic)
        addFunction(fBearer)
        addFunction(fHeader)
        addFunction(fQuery)
    }.build()

    private fun createSecurityFuns(cnAuthInfo: ClassName, mnAuthMap: MemberName, name: String, scheme: SecurityScheme): List<FunSpec> {
        val fnSet = "set $name".asFunctionName()
        val fnClear = "clear $name".asFunctionName()

        val fSet = poetFunSpec(fnSet) {
            when (scheme.mapToType()) {
                SecurityType.Basic -> {
                    addParameter("username", STRING)
                    addParameter("password", STRING)
                    addStatement("%N[%S] = %T.basic(username, password)", mnAuthMap, name, cnAuthInfo)
                }
                SecurityType.Bearer -> {
                    addParameter("token", STRING)
                    addStatement("%N[%S] = %T.bearer(token)", mnAuthMap, name, cnAuthInfo)
                }
                SecurityType.Header -> {
                    addParameter("apiKey", STRING)
                    addStatement("%N[%S] = %T.header(%S, apiKey)", mnAuthMap, name, cnAuthInfo, scheme.name)
                }
                SecurityType.Query -> {
                    addParameter("apiKey", STRING)
                    addStatement("%N[%S] = %T.query(%S, apiKey)", mnAuthMap, name, cnAuthInfo, scheme.name)
                }
                SecurityType.OAuth-> {
                    addParameter("oauth", STRING)
                    addStatement("%N[%S] = %T.header(%S, oauth)", mnAuthMap, name, cnAuthInfo, "Authorization")
                }
                SecurityType.Unknown -> {
                    throw IllegalStateException("SecurityScheme not supported: $scheme")
                }
            }
        }

        val fClear = poetFunSpec(fnClear) {
            addStatement("%N.remove(%S)", mnAuthMap, name)
        }

        return listOf(fSet, fClear)
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

}
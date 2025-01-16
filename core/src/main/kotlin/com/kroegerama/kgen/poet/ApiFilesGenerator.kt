package com.kroegerama.kgen.poet

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.Constants.AUTH_HEADER_VALUE_NAME_PREFIX
import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.language.*
import com.kroegerama.kgen.model.OperationWithInfo
import com.kroegerama.kgen.model.ResponseInfo
import com.kroegerama.kgen.model.SchemaWithMime
import com.kroegerama.kgen.openapi.OpenAPIAnalyzer
import com.kroegerama.kgen.openapi.OperationRequestType
import com.kroegerama.kgen.openapi.mapMimeToRequestType
import com.kroegerama.kgen.openapi.mapToParameterType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.parameters.Parameter

interface IApiFilesGenerator {
    fun getApiFiles(): List<FileSpec>
}

class ApiFilesGenerator(
    openAPI: OpenAPI,
    options: OptionSet,
    analyzer: OpenAPIAnalyzer
) : IApiFilesGenerator,
    IPoetGeneratorBase by PoetGeneratorBase(openAPI, options, analyzer),
    IPoetGeneratorSchemaHandler by PoetGeneratorSchemaHandler(openAPI, options, analyzer) {

    override fun getApiFiles(): List<FileSpec> = analyzer.apis.map { (name, operations) ->
        val apiName = "$name api"
        val cnHolder = ClassName(options.packageName, "ApiHolder")
        prepareFileSpec(options.apiPackage, apiName.asClassFileName()) {
            val className = ClassName(options.apiPackage, apiName.asTypeName())
            val apiInterface = poetInterface(className) {
                addAnnotation(JvmSuppressWildcards::class)

                val companion = TypeSpec.companionObjectBuilder()

                operations.forEach { opInfo ->
                    handleOperationInfo(cnHolder, className, this@poetInterface, companion, opInfo)
                }
                addType(companion.build())
            }
            addType(apiInterface)
        }
    }

    private fun handleOperationInfo(
        cnHolder: ClassName,
        apiClassName: ClassName,
        apiInterface: TypeSpec.Builder,
        companion: TypeSpec.Builder,
        operationInfo: OperationWithInfo
    ) {
        val funName = operationInfo.createOperationName().asFunctionName()
        val request = operationInfo.getRequest()
        val response = operationInfo.getResponse()

        val baseParams = collectParameters(operationInfo)
        val methodParams = request?.let { getAdditionalParameters(it) }.orEmpty()
        val allParameters = baseParams + methodParams

        val ifaceFun = generateIfaceFun(
            funName = funName,
            operationInfo = operationInfo,
            request = request,
            allParameters = allParameters,
            response = response,
            sync = false
        )
        val ifaceFunSync = generateIfaceFun(
            funName = funName,
            operationInfo = operationInfo,
            request = request,
            allParameters = allParameters,
            response = response,
            sync = true
        )

        val delegateFun = generateDelegateFun(
            funName = funName,
            operationInfo = operationInfo,
            allParameters = allParameters,
            response = response,
            cnHolder = cnHolder,
            apiClassName = apiClassName,
            sync = false,
            deprecated = operationInfo.deprecated
        )
        val delegateFunSync = generateDelegateFun(
            funName = funName,
            operationInfo = operationInfo,
            allParameters = allParameters,
            response = response,
            cnHolder = cnHolder,
            apiClassName = apiClassName,
            sync = true,
            deprecated = operationInfo.deprecated
        )

        apiInterface.addFunction(ifaceFun)
        apiInterface.addFunction(ifaceFunSync)
        companion.addFunction(delegateFun)
        companion.addFunction(delegateFunSync)
    }

    private fun collectParameters(operationInfo: OperationWithInfo) =
        operationInfo.operation.parameters.orEmpty().map { parameter ->
            createParameterSpecPair(parameter)
        }

    private fun createParameterSpecPair(parameter: Parameter): ParameterSpecPairInfo {
        val rawName = parameter.name
        val name = rawName.asFieldName()
        val schema = parameter.schema
        val paramType = parameter.mapToParameterType()

        val type = analyzer.findTypeNameFor(schema).let { typeName ->
            if (parameter.required) {
                typeName
            } else {
                typeName.copy(nullable = true)
            }
        }

        val ifaceParam = poetParameter(name, type) {
            addAnnotation(createParameterAnnotation(paramType, rawName))
        }
        val delegateParam = poetParameter(name, type) {
            //TODO add schema.default as defaultValue
            parameter.description?.let {
                addKdoc("%L", it)
            }
            if (!parameter.required) {
                defaultValue("null")
            }
        }
        return ParameterSpecPairInfo(ifaceParam, delegateParam)
    }

    private fun getAdditionalParameters(request: SchemaWithMime): List<ParameterSpecPairInfo> {
        val (mime, required, schema) = request
        return when (mime.mapMimeToRequestType()) {
            OperationRequestType.Default -> {
                val typeName = analyzer.findTypeNameFor(schema)
                val ifaceBodyParam = poetParameter("body", typeName.nullable(!required)) {
                    addAnnotation(PoetConstants.RETROFIT_BODY)
                }
                val delegateBodyParam = poetParameter("body", typeName.nullable(!required)) {
                    if (!required) defaultValue("null")
                    addAnnotation(PoetConstants.RETROFIT_BODY)
                }
                listOf(ParameterSpecPairInfo(ifaceBodyParam, delegateBodyParam))
            }

            OperationRequestType.Multipart -> {
                schema.convertToParameters(required, true)
            }

            OperationRequestType.UrlEncoded -> {
                schema.convertToParameters(required, false)
            }

            OperationRequestType.Unknown -> {
                //TODO!!
                emptyList()
            }
        }
    }

    private fun generateIfaceFun(
        funName: String,
        operationInfo: OperationWithInfo,
        request: SchemaWithMime?,
        allParameters: List<ParameterSpecPairInfo>,
        response: ResponseInfo?,
        sync: Boolean
    ) = poetFunSpec(funName.run {
        val suffix = if (sync) "Call" else ""
        "__$funName$suffix"
    }) {
        val methodAnnotation = createHttpMethodAnnotation(operationInfo.method, operationInfo.path, request != null)
        addAnnotation(methodAnnotation)

        if (operationInfo.securityNames.isNotEmpty()) {
            val cnInterceptor = ClassName(options.packageName, "ApiAuthInterceptor")
            val mnAuthHeader = MemberName(cnInterceptor, Constants.AUTH_HEADER_NAME)

            val secHeader = poetAnnotation(PoetConstants.RETROFIT_HEADERS) {
                operationInfo.securityNames.forEach { name ->
                    val mnHeaderValueName = MemberName(cnInterceptor, "$AUTH_HEADER_VALUE_NAME_PREFIX$name".asConstantName())

                    val block = buildCodeBlock {
                        add("\${%M}: \${%M}", mnAuthHeader, mnHeaderValueName)
                    }

                    addMember("\"%L\"", block)
                }
            }
            addAnnotation(secHeader)
        }

        when (request?.mime) {
            Constants.MIME_TYPE_MULTIPART_FORM_DATA -> addAnnotation(PoetConstants.RETROFIT_MULTIPART)
            Constants.MIME_TYPE_URL_ENCODED -> addAnnotation(PoetConstants.RETROFIT_FORM_ENCODED)
        }

        if (!sync) {
            addModifiers(KModifier.SUSPEND)
        }
        addModifiers(KModifier.ABSTRACT)
        addParameters(allParameters.map { it.ifaceParam }.sortedBy { ifaceParam ->
            //@Path must be defined before all other params
            ifaceParam.annotations.any { it.typeName != PoetConstants.RETROFIT_PARAM_PATH }
        }.sortedBy { ifaceParam ->
            //@Body must be the last param
            ifaceParam.annotations.any { it.typeName == PoetConstants.RETROFIT_BODY }
        })
        addReturns(response, false, sync)
    }

    private fun generateDelegateFun(
        funName: String,
        operationInfo: OperationWithInfo,
        allParameters: List<ParameterSpecPairInfo>,
        response: ResponseInfo?,
        cnHolder: ClassName,
        apiClassName: ClassName,
        sync: Boolean,
        deprecated: Boolean
    ) = poetFunSpec(funName.run {
        val suffix = if (sync) "Call" else ""
        "$funName$suffix"
    }) {
        operationInfo.operation.description?.let {
            addKdoc("%L", it)
        }
        if (!sync) {
            addModifiers(KModifier.SUSPEND)
        }
        if (deprecated) {
            addAnnotation(poetAnnotation(Deprecated::class.asClassName()) {
                addMember("message = %S", "Deprecated via OpenApi.")
            })
        }
        addParameters(allParameters.map { it.delegateParam })

        val paramList = parameters.joinToString(",\n    ", prefix = "\n", postfix = "\n") { it.name + " = " + it.name }
        val funNameSuffix = if (sync) "Call" else ""
        addStatement("return %T.getApi<%T>().%L(%L)", cnHolder, apiClassName, "__$funName$funNameSuffix", paramList)

        addReturns(response, true, sync)
    }

    private fun FunSpec.Builder.addReturns(responseInfo: ResponseInfo?, withDescription: Boolean, sync: Boolean) {
        responseInfo?.let { (_, description, schemaWithMime) ->
            val descriptionBlock = if (withDescription)
                CodeBlock.Builder().apply {
                    description?.let { add("%L", it) }
                }.build() else CodeBlock.builder().build()

            val responseBase = if (sync) PoetConstants.RETROFIT_CALL else PoetConstants.RETROFIT_RESPONSE
            schemaWithMime?.let { (mime, _, schema) ->
                val typeName = analyzer.findTypeNameFor(schema)
                val responseType = responseBase.parameterizedBy(typeName)

                if (mime == Constants.MIME_TYPE_JSON) {
                    returns(responseType, descriptionBlock)
                } else {
                    returns(
                        if (sync) PoetConstants.RETROFIT_CALL.parameterizedBy(PoetConstants.OK_RESPONSE_BODY) else PoetConstants.OK_RESPONSE_BODY,
                        descriptionBlock
                    )
                }
            } ?: returns(responseBase.parameterizedBy(UNIT), descriptionBlock)
        }
    }
}
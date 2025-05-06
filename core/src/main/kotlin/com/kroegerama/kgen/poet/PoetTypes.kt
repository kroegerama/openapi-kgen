package com.kroegerama.kgen.poet

import com.kroegerama.kgen.OptionSet
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class PoetTypes(
    private val options: OptionSet
) {
    val api = ClassName(options.packageName, "Api")
    val auth = ClassName(options.packageName, "Auth")

    fun modelName(vararg name: String) = ClassName(options.modelPackage, *name)

    fun apiServiceName(name: String) = ClassName(options.apiPackage, name + "Service")

    companion object {
        private const val OKHTTP_PACKAGE = "okhttp3"
        val HttpUrl = ClassName(OKHTTP_PACKAGE, "HttpUrl")
        val RequestBody = ClassName(OKHTTP_PACKAGE, "RequestBody")
        val ResponseBody = ClassName(OKHTTP_PACKAGE, "ResponseBody")
        val ListOfHttpUrl = LIST.parameterizedBy(HttpUrl)

        private const val COMPANION_PACKAGE = "com.kroegerama.kgen.companion"

        val ApiHolder = ClassName(COMPANION_PACKAGE, "ApiHolder")
        val AuthItem = ClassName(COMPANION_PACKAGE, "AuthItem")
        val AuthItemBasic = ClassName(COMPANION_PACKAGE, "AuthItem", "Basic")
        val AuthItemBearer = ClassName(COMPANION_PACKAGE, "AuthItem", "Bearer")
        val AuthItemApiKey = ClassName(COMPANION_PACKAGE, "AuthItem", "ApiKey")
        val AuthItemPosition = ClassName(COMPANION_PACKAGE, "AuthItem", "Position")
        val AuthInterceptorToken = ClassName(COMPANION_PACKAGE, "AuthInterceptorToken")

        val SerializableLocalDate = ClassName(COMPANION_PACKAGE, "SerializableLocalDate")
        val SerializableLocalTime = ClassName(COMPANION_PACKAGE, "SerializableLocalTime")
        val SerializableOffsetDateTime = ClassName(COMPANION_PACKAGE, "SerializableOffsetDateTime")
        val SerializableUUID = ClassName(COMPANION_PACKAGE, "SerializableUUID")
        val SerializableBase64 = ClassName(COMPANION_PACKAGE, "SerializableBase64")

        private const val COMPANION_RESPONSE_PACKAGE = "com.kroegerama.kgen.companion.response"
        private val CALL_EXCEPTION = ClassName(COMPANION_RESPONSE_PACKAGE, "CallException")
        private val CALL_RESPONSE = ClassName(COMPANION_RESPONSE_PACKAGE, "HttpCallResponse")

        private const val KTX_SERIALIZATION = "kotlinx.serialization"
        private const val KTX_SERIALIZATION_JSON = "kotlinx.serialization.json"

        val Serializable = ClassName(KTX_SERIALIZATION, "Serializable")
        val SerialName = ClassName(KTX_SERIALIZATION, "SerialName")
        val JsonElement = ClassName(KTX_SERIALIZATION_JSON, "JsonElement")
        val JsonClassDiscriminator = ClassName(KTX_SERIALIZATION_JSON, "JsonClassDiscriminator")

        private const val RETROFIT = "retrofit2.http"
        val HTTP = ClassName(RETROFIT, "HTTP")
        val Url = ClassName(RETROFIT, "Url")
        val Header = ClassName(RETROFIT, "Header")
        val Path = ClassName(RETROFIT, "Path")
        val Query = ClassName(RETROFIT, "Query")
        val Body = ClassName(RETROFIT, "Body")
        val PartMap = ClassName(RETROFIT, "PartMap")
        val FieldMap = ClassName(RETROFIT, "FieldMap")

        val PartMapType = MAP.parameterizedBy(STRING, RequestBody)
        val FieldMapType = MAP.parameterizedBy(STRING, STRING)

        val Deprecated = kotlin.Deprecated::class.asTypeName()
        val JvmSuppressWildcards = kotlin.jvm.JvmSuppressWildcards::class.asTypeName()
        val Immutable = ClassName("androidx.compose.runtime", "Immutable")

        private val Either = ClassName("arrow.core", "Either")

        fun either(cn: TypeName) = Either.parameterizedBy(
            CALL_EXCEPTION,
            CALL_RESPONSE.parameterizedBy(cn)
        )
    }
}

object PoetMembers {
    val ToHttpUrl = PoetTypes.HttpUrl.nestedClass("Companion").member("toHttpUrl")
}

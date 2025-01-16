package com.kroegerama.kgen.poet

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

object PoetConstants {

    private const val PKG_MOSHI = "com.squareup.moshi"
    private const val PKG_RETROFIT = "retrofit2"
    private const val PKG_RETROFIT_HTTP = "retrofit2.http"
    private const val PKG_OK = "okhttp3"

    val MOSHI_JSON = ClassName(PKG_MOSHI, "Json")
    val MOSHI_JSON_CLASS = ClassName(PKG_MOSHI, "JsonClass")
    val MOSHI = ClassName(PKG_MOSHI, "Moshi")
    val MOSHI_BUILDER = ClassName(PKG_MOSHI, "Moshi", "Builder")
    val MOSHI_JSON_ADAPTER = ClassName(PKG_MOSHI, "JsonAdapter")

    val JSON_READER = ClassName(PKG_MOSHI, "JsonReader")
    val JSON_WRITER = ClassName(PKG_MOSHI, "JsonWriter")

    val RETROFIT_HTTP = ClassName(PKG_RETROFIT_HTTP, "HTTP")

    val RETROFIT_MULTIPART = ClassName(PKG_RETROFIT_HTTP, "Multipart")
    val RETROFIT_FORM_ENCODED = ClassName(PKG_RETROFIT_HTTP, "FormUrlEncoded")
    val RETROFIT_HEADERS = ClassName(PKG_RETROFIT_HTTP, "Headers")

    val RETROFIT_PART = ClassName(PKG_RETROFIT_HTTP, "Part")
    val RETROFIT_FIELD = ClassName(PKG_RETROFIT_HTTP, "Field")
    val RETROFIT_BODY = ClassName(PKG_RETROFIT_HTTP, "Body")

    val RETROFIT_PARAM_HEADER = ClassName(PKG_RETROFIT_HTTP, "Header")
    val RETROFIT_PARAM_PATH = ClassName(PKG_RETROFIT_HTTP, "Path")
    val RETROFIT_PARAM_QUERY = ClassName(PKG_RETROFIT_HTTP, "Query")

    val RETROFIT_RESPONSE = ClassName(PKG_RETROFIT, "Response")

    val RETROFIT = ClassName(PKG_RETROFIT, "Retrofit")
    val RETROFIT_BUILDER = ClassName(PKG_RETROFIT, "Retrofit", "Builder")

    val RETROFIT_CREATE_FUN = MemberName(PKG_RETROFIT, "create")

    val RETROFIT_CALL = ClassName(PKG_RETROFIT, "Call")

    val CONVERTER = ClassName(PKG_RETROFIT, "Converter")
    val CONVERTER_FACTORY = ClassName(PKG_RETROFIT, "Converter", "Factory")

    val OK_REQUEST_BODY = ClassName(PKG_OK, "RequestBody")
    val OK_RESPONSE_BODY = ClassName(PKG_OK, "ResponseBody")
    val OK_MULTIPART_PART = ClassName(PKG_OK, "MultipartBody", "Part")

    val OK_CLIENT = ClassName(PKG_OK, "OkHttpClient")
    val OK_CLIENT_BUILDER = ClassName(PKG_OK, "OkHttpClient", "Builder")

    val OK_CREDENTIALS = ClassName(PKG_OK, "Credentials")
    val OK_INTERCEPTOR = ClassName(PKG_OK, "Interceptor")
    val OK_INTERCEPTOR_CHAIN = ClassName(PKG_OK, "Interceptor", "Chain")
    val OK_RESPONSE = ClassName(PKG_OK, "Response")

    val LIST_OF = MemberName("kotlin.collections", "listOf")
    val MUTABLE_MAP_OF = MemberName("kotlin.collections", "mutableMapOf")
    val EXCEPTION = ClassName("kotlin", "Exception")

    val DATE = Date::class.asClassName()
    val OFFSET_DATE_TIME = OffsetDateTime::class.asClassName()
    val LOCAL_DATE = LocalDate::class.asClassName()

    val RFC_DATE_ADAPTER = ClassName("com.squareup.moshi.adapters", "Rfc3339DateJsonAdapter")

    val SEALED_TYPE_LABEL = ClassName("dev.zacsweers.moshix.sealed.annotations", "TypeLabel")

    val COMPOSE_IMMUTABLE = ClassName("androidx.compose.runtime", "Immutable")
}
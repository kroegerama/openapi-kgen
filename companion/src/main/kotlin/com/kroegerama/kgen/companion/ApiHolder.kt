package com.kroegerama.kgen.companion

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create

class ApiHolder(
    baseUrl: HttpUrl
) {
    val authInterceptor = AuthInterceptor()

    var json = createJson()
        private set
    var client = createClient { addInterceptor(authInterceptor) }
        private set
    var retrofit = createRetrofit(baseUrl = baseUrl, json = json, client = client)
        private set

    fun updateJson(json: Json) {
        this.json = json
        retrofit = createRetrofit(
            baseUrl = retrofit.baseUrl(),
            json = json,
            client = client
        )
        serviceCache.clear()
    }

    fun updateClient(decorator: OkHttpClient.Builder.() -> Unit) {
        client = createClient {
            addInterceptor(authInterceptor)
            decorator()
        }
        retrofit = createRetrofit(
            baseUrl = retrofit.baseUrl(),
            json = json,
            client = client
        )
        serviceCache.clear()
    }

    fun updateRetrofit(decorator: Retrofit.Builder.() -> Unit) {
        retrofit = createRetrofit(
            baseUrl = retrofit.baseUrl(),
            json = json,
            client = client,
            decorator = decorator
        )
        serviceCache.clear()
    }

    @PublishedApi
    internal val serviceCache: MutableMap<Class<*>, Any> = mutableMapOf()

    inline fun <reified T : Any> getService(): T = (serviceCache.getOrPut(T::class.java) {
        retrofit.create<T>()
    } as T)

    companion object {
        fun createJson() = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true

            explicitNulls = false
            coerceInputValues = true
        }

        fun createClient(
            decorator: OkHttpClient.Builder.() -> Unit = {}
        ) = OkHttpClient.Builder()
            .apply(decorator)
            .build()

        fun createRetrofit(
            baseUrl: HttpUrl,
            json: Json,
            client: OkHttpClient,
            decorator: Retrofit.Builder.() -> Unit = {}
        ) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addCallAdapterFactory(EitherCallAdapterFactory)
            .addConverterFactory(StringConverterFactory)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .apply(decorator)
            .build()
    }
}

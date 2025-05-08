package com.kroegerama.kgen.companion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.Interceptor
import retrofit2.Invocation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthInterceptorToken(
    vararg val value: String
)

typealias AuthItemProvider = suspend () -> AuthItem?

class AuthInterceptor : Interceptor {
    private val authProviderMap: MutableMap<String, AuthItemProvider> = mutableMapOf()

    fun setAuthProvider(id: String, provider: AuthItemProvider) {
        authProviderMap[id] = provider
    }

    fun clearAuthProvider(id: String) {
        authProviderMap.remove(id)
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()

        val invocationMethod = request.tag(Invocation::class.java)?.method() ?: return chain.proceed(request)
        val token = invocationMethod.getAnnotation(AuthInterceptorToken::class.java) ?: return chain.proceed(request)
        if (token.value.isEmpty()) return chain.proceed(request)

        val newRequest = request.newBuilder().apply {
            val urlBuilder = request.url.newBuilder()
            for (parameterName in token.value) {
                val provider = authProviderMap[parameterName] ?: continue
                val authItem = runBlocking(Dispatchers.IO) { provider() } ?: continue

                when (authItem.position) {
                    AuthItem.Position.Header -> addHeader(authItem.name, authItem.value)
                    AuthItem.Position.Query -> urlBuilder.addQueryParameter(authItem.name, authItem.value)
                    AuthItem.Position.Cookie -> addHeader("Cookie", authItem.run { "$name=$value" })
                }
            }
            url(urlBuilder.build())
        }.build()
        return chain.proceed(newRequest)
    }
}

sealed interface AuthItem {
    val position: Position
    val name: String
    val value: String

    data class Basic(
        val username: String,
        val password: String
    ) : AuthItem {
        override val position = Position.Header
        override val name: String = "Authorization"
        override val value: String = Credentials.basic(username, password)
    }

    data class Bearer(
        val token: String,
        val prefix: String = "Bearer"
    ) : AuthItem {
        override val position = Position.Header
        override val name: String = "Authorization"
        override val value: String = "$prefix $token"
    }

    data class ApiKey(
        override val position: Position,
        override val name: String,
        override val value: String
    ) : AuthItem

    enum class Position {
        Header, Query, Cookie
    }
}

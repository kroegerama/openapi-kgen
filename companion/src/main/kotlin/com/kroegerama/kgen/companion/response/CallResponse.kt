package com.kroegerama.kgen.companion.response

import androidx.compose.runtime.Immutable
import arrow.core.Either
import arrow.core.merge
import okhttp3.Headers
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

typealias EitherCallResponse<T> = Either<CallException, HttpCallResponse<T>>
typealias EitherTypedCallResponse<E, T> = Either<TypedCallException<E>, HttpCallResponse<T>>

fun Response<*>.asCallException(): CallException {
    return HttpCallException(
        raw = raw(),
        cause = null
    )
}

fun Throwable.asCallException(): CallException {
    return when (this) {
        is HttpException -> HttpCallException(
            raw = response()?.raw() ?: return UnexpectedCallException("Response was null", this),
            cause = this
        )

        is IOException -> IOCallException(null, this)
        else -> UnexpectedCallException(null, this)
    }
}

fun <T> EitherCallResponse<T>.asResponse(): Response<out T> = mapLeft<Response<T>> { callException ->
    val code: Int = when (callException) {
        is HttpCallException -> callException.code
        is IOCallException -> 999
        is UnexpectedCallException -> 999
    }
    val responseBody: ResponseBody = when (callException) {
        is HttpCallException -> callException.raw.body
        is IOCallException -> null
        is UnexpectedCallException -> null
    } ?: callException.message.orEmpty().toResponseBody()
    Response.error(code, responseBody)
}.map<Response<T>> {
    Response.success(it.data, it.raw)
}.merge()

@Immutable
data class HttpCallResponse<out T>(
    val data: T,
    val raw: okhttp3.Response
) {
    val code: Int = raw.code
    val message: String = raw.message
    val headers: Headers = raw.headers
    val isSuccessful: Boolean = raw.isSuccessful
}

@Immutable
sealed interface TypedCallException<out E>

@Immutable
sealed class CallException : RuntimeException(), TypedCallException<Nothing>

data class TypedHttpCallException<out E>(
    val error: E,
    val raw: okhttp3.Response,
    val cause: CallException?
) : TypedCallException<E> {
    val code: Int = raw.code
    val headers: Headers = raw.headers
}

data class HttpCallException(
    val raw: okhttp3.Response,
    override val cause: Throwable?
) : CallException() {
    val code: Int = raw.code
    override val message: String = raw.message
    val headers: Headers = raw.headers
}

data class IOCallException(
    override val message: String?,
    override val cause: IOException
) : CallException()

data class UnexpectedCallException(
    override val message: String?,
    override val cause: Throwable?
) : CallException()

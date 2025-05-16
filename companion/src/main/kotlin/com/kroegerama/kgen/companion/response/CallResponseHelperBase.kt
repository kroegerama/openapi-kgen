package com.kroegerama.kgen.companion.response

import arrow.core.Either
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.lang.reflect.Type

open class CallResponseHelperBase<ErrorType>(
    private val errorType: Class<ErrorType>,
    private val retrofitProvider: () -> Retrofit
) {
    private val retrofit get() = retrofitProvider()

    fun <T> EitherCallResponse<T>.typed(): EitherTypedCallResponse<ErrorType, T> {
        return mapLeft { callException ->
            callException.typed()
        }
    }

    fun CallException.typed(): TypedCallException<ErrorType> = typed(retrofit, errorType)
}

inline fun <reified E> CallException.typed(
    retrofit: Retrofit
): TypedCallException<E> = typed(retrofit, E::class.java)

fun <E> CallException.typed(
    retrofit: Retrofit,
    type: Type
): TypedCallException<E> {
    if (this !is HttpCallException) {
        return this
    }
    val error = response.errorBody()?.convert<E>(retrofit, type)?.getOrNull() ?: return this
    return TypedHttpCallException(
        error = error,
        response = response,
        cause = this
    )
}

inline fun <reified E> ResponseBody.convert(
    retrofit: Retrofit
): Either<Throwable, E> = convert(retrofit, E::class.java)

fun <E> ResponseBody.convert(
    retrofit: Retrofit,
    type: Type
): Either<Throwable, E> = Either.catch {
    val converter = retrofit.responseBodyConverter<E>(type, arrayOfNulls(0))
    converter.convert(this)!!
}

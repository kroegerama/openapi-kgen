package com.kroegerama.kgen.companion.response

import arrow.core.Either
import okhttp3.ResponseBody
import retrofit2.Retrofit

open class CallResponseHelperBase<ErrorType>(
    private val errorType: Class<ErrorType>,
    private val retrofitProvider: () -> Retrofit
) {
    private val retrofit get() = retrofitProvider()

    fun <T> EitherCallResponse<T>.typed(): EitherTypedCallResponse<ErrorType, T> {
        return mapLeft<TypedCallException<ErrorType>> { callException ->
            when (callException) {
                is HttpCallException -> {
                    val error = callException.raw.body?.convert()?.getOrNull() ?: return@mapLeft callException
                    TypedHttpCallException(
                        error = error,
                        raw = callException.raw,
                        cause = callException
                    )
                }

                is IOCallException -> callException
                is UnexpectedCallException -> callException
            }
        }
    }

    private fun ResponseBody.convert(): Either<Throwable, ErrorType> = Either.catch {
        val converter = retrofit.responseBodyConverter<ErrorType>(errorType, arrayOfNulls(0))
        converter.convert(this)!!
    }
}

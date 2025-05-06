package com.kroegerama.kgen.companion

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.kroegerama.kgen.companion.response.CallException
import com.kroegerama.kgen.companion.response.HttpCallResponse
import com.kroegerama.kgen.companion.response.UnexpectedCallException
import com.kroegerama.kgen.companion.response.asCallException
import okhttp3.Request
import okio.Timeout
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Call adapter for retrofit to allow returning `arrow.Either<>` objects.
 * Declare the response of the methods in the service interface like this:
 * * `Either<CallException, CallResponse<JsonElement>>`
 * * `Either<CallException, CallResponse<MyType>>`
 * * `Either<CallException, CallResponse<Unit>>`
 */
object EitherCallAdapterFactory : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        val rawReturnType = getRawType(returnType)
        if (rawReturnType != Call::class.java) return null

        val callType = getParameterUpperBound(0, returnType as ParameterizedType)
        val rawType = getRawType(callType)
        if (rawType != Either::class.java) {
            return null
        }

        callType as ParameterizedType
        val leftType = getParameterUpperBound(0, callType)
        val leftClass = getRawType(leftType)

        if (leftClass != CallException::class.java) {
            throw IllegalArgumentException(
                "Either type ($leftType) is wrong; ${CallException::class.java.simpleName} type must be placed in the left side like " +
                        "Either<${CallException::class.java.simpleName}, ${HttpCallResponse::class.java.simpleName}<T>>",
            )
        }

        val rightType = getParameterUpperBound(1, callType)
        val rightClass = getRawType(rightType)

        if (rightClass != HttpCallResponse::class.java) {
            throw IllegalArgumentException(
                "Either type ($rightType) is wrong; ${HttpCallResponse::class.java.simpleName} type must be placed in the right side like " +
                        "Either<${CallException::class.java.simpleName}, ${HttpCallResponse::class.java.simpleName}<T>>",
            )
        }

        rightType as ParameterizedType
        val paramType = getParameterUpperBound(0, rightType)

        return EitherCallAdapter(
            paramType = paramType,
        )
    }

    internal class EitherCallAdapter(
        private val paramType: Type
    ) : CallAdapter<Type, Call<Either<CallException, HttpCallResponse<Type>>>> {

        override fun responseType(): Type = paramType

        override fun adapt(call: Call<Type>): Call<Either<CallException, HttpCallResponse<Type>>> {
            return EitherCall(call, paramType)
        }
    }

    internal class EitherCall<T : Any>(
        private val proxy: Call<T>,
        private val paramType: Type
    ) : Call<Either<CallException, HttpCallResponse<T>>> {

        override fun enqueue(callback: Callback<Either<CallException, HttpCallResponse<T>>>) {
            val proxyCallback = object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) =
                    callback.onResponse(
                        this@EitherCall,
                        convertResponse(response)
                    )

                override fun onFailure(call: Call<T>, t: Throwable) =
                    callback.onResponse(
                        this@EitherCall,
                        convertThrowable(t)
                    )
            }
            proxy.enqueue(proxyCallback)
        }

        override fun execute(): Response<Either<CallException, HttpCallResponse<T>>> {
            throw UnsupportedOperationException("This adapter does not support sync execution")
        }

        private fun convertThrowable(throwable: Throwable): Response<Either<CallException, HttpCallResponse<T>>> {
            return Response.success(throwable.asCallException().left())
        }

        private fun <T> convertResponse(response: Response<T>): Response<Either<CallException, HttpCallResponse<T>>> {
            val convertedBody: Either<CallException, HttpCallResponse<T>> = either {
                if (!response.isSuccessful) {
                    raise(
                        response.asCallException()
                    )
                }

                val isUnit = paramType == Unit::class.java

                if (response.code() == 204) {
                    if (isUnit) {
                        @Suppress("UNCHECKED_CAST")
                        return@either HttpCallResponse(
                            data = Unit as T,
                            raw = response.raw()
                        )
                    }
                    raise(
                        UnexpectedCallException(
                            message = "Response code is ${response.code()} and body is null but <T> is $paramType. <T> needs to be Unit.",
                            cause = null
                        )
                    )
                }

                val data = Either.catch {
                    @Suppress("UNCHECKED_CAST")
                    if (isUnit) {
                        Unit as T
                    } else {
                        response.body() as T
                    }
                }.mapLeft {
                    UnexpectedCallException(
                        message = null,
                        cause = it
                    )
                }.bind()
                HttpCallResponse(
                    data = data,
                    raw = response.raw()
                )
            }
            return Response.success(convertedBody)
        }

        override fun clone(): Call<Either<CallException, HttpCallResponse<T>>> = EitherCall(proxy.clone(), paramType)
        override fun request(): Request = proxy.request()
        override fun timeout(): Timeout = proxy.timeout()
        override fun isExecuted(): Boolean = proxy.isExecuted
        override fun isCanceled(): Boolean = proxy.isCanceled
        override fun cancel() = proxy.cancel()
    }
}

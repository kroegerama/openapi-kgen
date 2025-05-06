package com.kroegerama.kgen.companion

//typealias EitherCallResponse<T> = Either<CallException, HttpCallResponse<T>>
//typealias EitherTypedCallResponse<E, T> = Either<TypedCallException<E>, HttpCallResponse<T>>
//
//fun Response<*>.asCallException(): CallException {
//    return HttpCallException(
//        code = code(),
//        response = this,
//        message = "HTTP response code ${code()}",
//        cause = null
//    )
//}
//
//fun Throwable.asCallException(): CallException = when (this) {
//    is HttpException -> HttpCallException(
//        code = code(),
//        response = response(),
//        message = message(),
//        cause = this
//    )
//
//    is IOException -> IOCallException(null, this)
//    else -> UnexpectedCallException(null, this)
//}
//
//fun <T> EitherCallResponse<T>.asResponse(): Response<out T> = mapLeft { callException ->
//    @Suppress("UNCHECKED_CAST")
//    when (callException) {
//        is HttpCallException -> callException.response as Response<out T>
//        is IOCallException -> Response.error(999, callException.message.orEmpty().toResponseBody())
//        is UnexpectedCallException -> Response.error(999, callException.message.orEmpty().toResponseBody())
//    }
//}.map {
//    it.response
//}.merge()
//
//@Immutable
//sealed interface TypedCallException<out E>
//
//@Immutable
//sealed class CallException : RuntimeException(), TypedCallException<Nothing>
//
//data class TypedHttpCallException<out E>(
//    val error: E,
//    val code: Int,
//    val response: Response<*>?,
//    val cause: CallException?
//) : TypedCallException<E>
//
//data class HttpCallException(
//    val code: Int,
//    val response: Response<*>?,
//    override val message: String?,
//    override val cause: Throwable?
//) : CallException()
//
//data class IOCallException(
//    override val message: String?,
//    override val cause: IOException
//) : CallException()
//
//data class UnexpectedCallException(
//    override val message: String?,
//    override val cause: Throwable?
//) : CallException()
//
//@Immutable
//data class HttpCallResponse<out T>(
//    val data: T,
//    val response: Response<out T>
//) {
//    val code = response.code()
//}

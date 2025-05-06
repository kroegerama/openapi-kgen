package com.kroegerama.kgen.gradle

import org.gradle.api.provider.Provider
import java.util.*

/**
 * credits to https://github.com/gradle/gradle/issues/12388#issuecomment-2132377695
 */
inline fun <T : Any, S : Any> Provider<T>.mapKt(
    crossinline transformer: (T) -> S?
): Provider<S> = map { value: T ->
    Optional.ofNullable(transformer(value))
}.filter(Optional<S>::isPresent).map(Optional<S>::get)

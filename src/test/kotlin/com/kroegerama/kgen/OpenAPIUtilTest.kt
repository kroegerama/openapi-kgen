package com.kroegerama.kgen

import com.kroegerama.kgen.openapi.isNullable
import org.junit.Assert
import org.junit.Test

class OpenAPIUtilTest {

    private val nullableCases = listOf<Triple<Boolean?, Boolean?, Boolean>>(
        Triple(false, null, true),
        Triple(false, false, true),
        Triple(false, true, true),
        Triple(true, null, false),
        Triple(true, false, false),
        Triple(true, true, true)
        )

    @Test
    fun testIsNullable() {

        nullableCases.forEach { (required, nullable, expected) ->
            val result = isNullable(required, nullable)
            Assert.assertEquals("required = $required, nullable = $nullable", expected, result)
        }

    }

}
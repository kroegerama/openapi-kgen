package com.kroegerama.kgen

import com.kroegerama.kgen.language.lowerCamelCase
import com.kroegerama.kgen.language.lowerSnakeCase
import com.kroegerama.kgen.language.upperCamelCase
import com.kroegerama.kgen.language.upperSnakeCase
import org.junit.Assert.assertEquals
import org.junit.Test


class CaseUtilTest {

    @Test
    fun testCamelCase() {
        val expected = "HalloWelt"
        assertEquals(expected, expected.upperCamelCase())
        assertEquals(expected, "halloWelt".upperCamelCase())
        assertEquals(expected, "hallo_welt".upperCamelCase())
        assertEquals(expected, "hallo.welt".upperCamelCase())
        assertEquals(expected, "-hallo_welt".upperCamelCase())
        assertEquals(expected, "hallo_.welt".upperCamelCase())
        assertEquals(expected, "__hallo.welt".upperCamelCase())

        assertEquals("SetupGui", "setup_gui".upperCamelCase())

        assertEquals("setupGUI", "setupGUI".lowerCamelCase())

        assertEquals("HalloWelt0", "__hallo welt_0".upperCamelCase())
    }

    @Test
    fun testSnakeCase() {
        assertEquals("hallo_welt", "halloWelt".lowerSnakeCase())
        assertEquals("hallo_welt", "hallo.welt".lowerSnakeCase())
        assertEquals("hallo_welt", "-hallo_welt".lowerSnakeCase())
        assertEquals("hallo_welt", "hallo_.welt".lowerSnakeCase())
        assertEquals("hallo_welt", "hallo.welt".lowerSnakeCase())
        assertEquals("setup_gui", "setupGUI".lowerSnakeCase())

        assertEquals("test_method_0", "testMethod0".lowerSnakeCase())
        assertEquals("hallo_welt_0", "__hallo welt_0".lowerSnakeCase())

        assertEquals("HALLO_WELT", "halloWelt".upperSnakeCase())
    }

}
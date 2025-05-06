package com.kroegerama.kgen.companion

import java.util.*

object BuildConfig {
    private val properties by lazy {
        Properties().apply {
            try {
                load(BuildConfig.javaClass.getResourceAsStream("/version.properties"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val version: String by lazy { properties.getProperty("version", "<<unknown>>") }
}

package com.kroegerama.kgen

import io.swagger.v3.oas.models.info.Info
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object Util {
    private val dateTime by lazy {
        ZonedDateTime.now()
    }
    val unixDateTime by lazy {
        dateTime.toEpochSecond()
    }
    val formattedDateTime: String by lazy {
        dateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME)
    }
    val generatorInfo by lazy {
        "OpenAPI KGen (version %s) by kroegerama".format(BuildConfig.version)
    }
}

fun String.sanitizePath() = trimStart('/')

fun String.asBaseUrl(): String {
    val hasSchema = startsWith("http://") || startsWith("https://")
    val hasEndSlash = endsWith("/")

    val prefix = if (hasSchema) "" else "http://"
    val suffix = if (hasEndSlash) "" else "/"

    return "$prefix$this$suffix"
}

fun Info.asFileHeader() = buildString {
    appendln(Constants.FILE_HEADER_NOTE)
    appendln()
    appendln(title)
    description?.let { appendln(it) }
    appendln("Version $version")
    contact?.run {
        appendln()
        appendln("Contact")
        name?.let { appendln("  $it") }
        email?.let { appendln("  Mail: $it") }
        url?.let { appendln("  URL: $it") }
    }
    license?.run {
        appendln()
        appendln("Spec License")
        name?.let { appendln("  $it") }
        url?.let { appendln("  $it") }
    }
    appendln()
    appendln("Generated ${Util.formattedDateTime}")
    appendln(Util.generatorInfo)
}
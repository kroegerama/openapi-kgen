package com.kroegerama.kgen

fun String.sanitizePath() = trimStart('/')

fun String.asBaseUrl(): String {
    val hasSchema = startsWith("http://") || startsWith("https://")
    val hasEndSlash = endsWith("/")

    val prefix = if (hasSchema) "" else "http://"
    val suffix = if (hasEndSlash) "" else "/"

    return "$prefix$this$suffix"
}

fun String.asMultilineComment() = trim().split("\n").joinToString(
    separator = "\n * ",
    prefix = "/* \n * ",
    postfix = "\n */"
) {
    it.replace("*/","*\u200B/")
}

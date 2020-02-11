package com.kroegerama.kgen.language

import com.ibm.icu.text.RuleBasedNumberFormat
import java.util.*

fun String.asFunctionName() = fixLeadingNumber().lowerCamelCase()
fun String.asFieldName() = fixLeadingNumber().lowerCamelCase()
fun String.asTypeName() = fixLeadingNumber().upperCamelCase()
fun String.asConstantName() = fixLeadingNumber().upperSnakeCase()
fun String.asClassFileName() = fixLeadingNumber().upperCamelCase()

private val leadingNumberRegex by lazy { "^([0-9]+)(.*)".toRegex() }
private val ruleNumberFormat by lazy { RuleBasedNumberFormat(Locale.US, RuleBasedNumberFormat.SPELLOUT) }

fun String.fixLeadingNumber(): String = if (startsWithNumber()) {
    val tail = leadingNumberRegex.replace(this, "$2")
    val num = substring(0, length - tail.length).toLongOrNull()
    if (num == null) {
        tail
    } else {
        ruleNumberFormat.format(num) + tail.capitalize()
    }
} else this

fun String.startsWithNumber() = leadingNumberRegex.matches(this)

fun String.escapeKotlin() =
    if (this in reservedWords)
        "`$this`"
    else
        this

/**
 * DOES NOT contain keywords from https://kotlinlang.org/docs/reference/keyword-reference.html
 * because 'val data: String' is valid, but not 'val fun: String'
 * also valid: 'val override: Int = 2'
 */
val reservedWords = setOf(
    "package",
    "as",
    "typealias",
    "class",
    "this",
    "super",
    "val",
    "var",
    "fun",
    "for",
    "null",
    "true",
    "false",
    "is",
    "in",
    "throw",
    "return",
    "break",
    "continue",
    "object",
    "if",
    "try",
    "else",
    "while",
    "do",
    "when",
    "interface",
    "typeof"
)
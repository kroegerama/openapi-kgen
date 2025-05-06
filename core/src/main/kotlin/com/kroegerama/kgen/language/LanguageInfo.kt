package com.kroegerama.kgen.language

// Handle snake_case, kebab-case, and non-alphanumeric characters
private val nonWord = "[^a-zA-Z0-9]".toRegex()

// Regex to match words in CamelCase, including uppercase acronyms like "CSVExport"
private val splitter = "([A-Z]+(?![a-z])|[A-Z]?[a-z]+|\\d+)".toRegex()

private fun String.splitHelper(
    partModifier: (String) -> String,
    separator: String,
    firstCharModifier: (Char) -> Char
): String {
    val cleaned = replace(nonWord, " ")
    val words = splitter.findAll(cleaned).map { it.value }.toList()
    val combined = words.joinToString(
        separator = separator,
        transform = partModifier
    ).replaceFirstChar(firstCharModifier)
    return if (combined.firstOrNull()?.isDigit() == true) "_$combined" else combined
}

fun String.asFunctionName() = splitHelper(
    partModifier = { part ->
        part.lowercase().replaceFirstChar {
            it.uppercase()
        }
    },
    separator = "",
    firstCharModifier = {
        it.lowercaseChar()
    }
)

fun String.asFieldName() = asFunctionName()

fun String.asTypeName() = splitHelper(
    partModifier = { part ->
        part.replaceFirstChar {
            it.uppercase()
        }
    },
    separator = "",
    firstCharModifier = { it }
)

fun String.asConstantName() = splitHelper(
    partModifier = { part ->
        part.uppercase()
    },
    separator = "_",
    firstCharModifier = { it }
)

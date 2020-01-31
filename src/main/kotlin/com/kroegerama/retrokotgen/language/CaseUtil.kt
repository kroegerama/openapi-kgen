package com.kroegerama.retrokotgen.language

fun String.upperCamelCase() = splitWordsForCase().joinToString("") { it.capitalize() }
fun String.lowerCamelCase() = upperCamelCase().decapitalize()

fun String.lowerSnakeCase() = splitWordsForCase().joinToString("_") { it.toLowerCase() }
fun String.upperSnakeCase() = lowerSnakeCase().toUpperCase()

private fun String.splitWordsForCase() =
    replace("([a-z])([A-Z])".toRegex(), "$1.$2")          //separate CamelCase words
        .replace("([a-zA-Z])([0-9])".toRegex(), "$1.$2")  //separate words with following number
        .replace("([0-9])([a-zA-Z])".toRegex(), "$1.$2")  //separate numbers with following word
        .split("[^a-zA-Z0-9]+".toRegex())                            //split at non word char
        .filter { !it.isBlank() }                                    //remove empty parts
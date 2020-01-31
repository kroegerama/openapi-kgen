package com.kroegerama.retrokotgen.cli

data class OptionSet(
    val specFile: String,
    val packageName: String,
    val outputDir: String,
    val limitApis: Set<String>,
    val verbose: Boolean,
    val dryRun: Boolean,
    val useInlineClass: Boolean
) {

    val apiPackage = "$packageName.api"
    val modelPackage = "$packageName.models"

}
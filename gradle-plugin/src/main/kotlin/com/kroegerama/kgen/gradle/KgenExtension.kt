package com.kroegerama.kgen.gradle

import java.io.File

open class KgenExtension {
    var specFile: File? = null
    var specUri: String? = null
    var packageName: String? = null
    var limitApis = emptySet<String>()
    var useInlineClasses = false
    var useCompose = false

    override fun toString(): String {
        return "KgenExtension(specFile=$specFile, specUri=$specUri, packageName='$packageName', limitApis=$limitApis, useInlineClasses=$useInlineClasses, useCompose=$useCompose)"
    }

}
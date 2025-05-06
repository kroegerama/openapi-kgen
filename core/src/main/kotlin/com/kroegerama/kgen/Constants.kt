package com.kroegerama.kgen

import BuildConfig

object Constants {
    const val CLI_NAME = "openapi-kgen"
    const val DEFAULT_PACKAGE_NAME = "com.kroegerama.retrokotgen.generated"

    const val MIME_TYPE_JSON = "application/json"
    const val MIME_TYPE_MULTIPART_FORM_DATA = "multipart/form-data"
    const val MIME_TYPE_URL_ENCODED = "application/x-www-form-urlencoded"

    const val FALLBACK_TAG = "default"

    const val SRC_PATH = "src/main/kotlin"

    const val FILE_HEADER_NOTE = "NOTE: This file is auto generated. Do not edit the file manually!"

    const val TASK_GROUP = "kgen"
    const val TASK_DESCRIPTION = "Generates source files from the OpenAPI Spec"

    const val EXT_FORCE_CREATE = "x-kgen-force-create"

    val generatorInfo: String = "OpenAPI KGen (version %s) by kroegerama".format(BuildConfig.COMPANION)
}

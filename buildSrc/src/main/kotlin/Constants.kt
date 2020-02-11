object C {

    const val PROJECT_NAME = "OpenAPI KGen"
    const val PROJECT_DESCRIPTION = "Generate modern API Clients in Kotlin from OpenAPI specifications. Supports OpenAPI >= 3.0.0."

    const val PROJECT_VERSION = "0.9.1"
    const val PROJECT_GROUP_ID = "com.kroegerama.openapi-kgen"

    const val PROJECT_URL = "https://github.com/kroegerama/openapi-kgen"

}

object V {
    const val KOTLIN = "1.3.61"
    const val BINTRAY = "1.8.4"
    const val GRADLE_PUBLISH = "0.10.1"
    const val ANDROID_GRADLE = "3.5.3"
    const val AIRLINE = "0.8"
    const val GUAVA = "28.1-jre"
    const val ICU = "65.1"
    const val SWAGGER_PARSER = "2.0.17"
    const val KOTLIN_POET = "1.5.0"
}

object Dep {
    const val KOTLIN_GRADLE = "org.jetbrains.kotlin:kotlin-gradle-plugin:${V.KOTLIN}"
    const val ANDROID_GRADLE = "com.android.tools.build:gradle:${V.ANDROID_GRADLE}"

    const val AIRLINE = "io.airlift:airline:${V.AIRLINE}"
    const val GUAVA = "com.google.guava:guava:${V.GUAVA}"

    const val ICU = "com.ibm.icu:icu4j:${V.ICU}"
    const val SWAGGER_PARSER = "io.swagger.parser.v3:swagger-parser:${V.SWAGGER_PARSER}"
    const val KOTLIN_POET = "com.squareup:kotlinpoet:${V.KOTLIN_POET}"
}
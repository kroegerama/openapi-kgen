plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    alias(libs.plugins.gradle.publish)
}

dependencies {
    implementation(project(":core"))

    implementation(gradleApi())
    implementation(libs.kotlin.gradle.plugin)
    compileOnly(libs.agp)
}

gradlePlugin {
    plugins {
        create("kgenPlugin") {
            id = "com.kroegerama.openapi-kgen.gradle-plugin"
            implementationClass = "com.kroegerama.kgen.gradle.KgenPlugin"
            displayName = C.PROJECT_NAME

            website.set("https://github.com/kroegerama/openapi-kgen")
            vcsUrl.set("https://github.com/kroegerama/openapi-kgen.git")
            description = C.PROJECT_DESCRIPTION
            tags.set(listOf("openapi", "generator", "codegen", "swagger"))
        }
    }
}

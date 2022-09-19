plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version V.GRADLE_PUBLISH
}

dependencies {
    implementation(project(":core"))

    implementation(gradleApi())
    compileOnly(Dep.ANDROID_GRADLE)
    implementation(Dep.KOTLIN_GRADLE)
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jetbrains.trove4j:trove4j:20160824")).using(module("org.jetbrains.intellij.deps:trove4j:1.0.20181211"))
    }
}

gradlePlugin {
    plugins {
        create("kgenPlugin") {
            id = "com.kroegerama.openapi-kgen.gradle-plugin"
            implementationClass = "com.kroegerama.kgen.gradle.KgenPlugin"
            displayName = C.PROJECT_NAME
        }
    }
}

pluginBundle {
    website = "https://github.com/kroegerama/openapi-kgen"
    vcsUrl = "https://github.com/kroegerama/openapi-kgen.git"
    description = C.PROJECT_DESCRIPTION
    tags = listOf("openapi", "generator", "codegen", "swagger")
}

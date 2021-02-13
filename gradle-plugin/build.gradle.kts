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

gradlePlugin {
    plugins {
        create("kgenPlugin") {
            id = "com.kroegerama.openapi-kgen.gradle-plugin"
            implementationClass = "com.kroegerama.kgen.gradle.KgenPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/kroegerama/openapi-kgen"
    vcsUrl = "https://github.com/kroegerama/openapi-kgen.git"
    description = C.PROJECT_DESCRIPTION
    tags = listOf("openapi", "generator", "codegen", "swagger")

    plugins {
        named("kgenPlugin") {
            id = "com.kroegerama.openapi-kgen.gradle-plugin"
            displayName = "${C.PROJECT_NAME} Gradle Plugin"
            description = C.PROJECT_DESCRIPTION
        }
    }
    mavenCoordinates {
        groupId = "com.kroegerama.openapi-kgen"
        artifactId = "gradle-plugin"
    }
}
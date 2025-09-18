dependencies {
    implementation(Dep.ICU)

    api(Dep.SWAGGER_PARSER)
    implementation(Dep.KOTLIN_POET)
    implementation(Dep.OKHTTP)
}

tasks.processResources {
    filesMatching("version.properties") {
        expand(project.properties)
    }
}
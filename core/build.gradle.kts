dependencies {
    implementation(Dep.ICU)

    api(Dep.SWAGGER_PARSER)
    implementation(Dep.KOTLIN_POET)
}

tasks.processResources {
    filesMatching("version.properties") {
        expand(project.properties)
    }
}
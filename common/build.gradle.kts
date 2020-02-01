dependencies {
    implementation("com.ibm.icu:icu4j:65.1")

    api("io.swagger.parser.v3:swagger-parser:2.0.17")
    implementation("com.squareup:kotlinpoet:1.5.0")
}

tasks.processResources {
    filesMatching("version.properties") {
        expand(project.properties)
    }
}
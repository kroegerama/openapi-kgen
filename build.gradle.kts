plugins {
    java
    kotlin("jvm") version V.KOTLIN
    id("signing")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    apply {
        plugin("maven-publish")
        plugin("signing")
        plugin("org.jetbrains.kotlin.jvm")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    version = C.PROJECT_VERSION
    group = C.PROJECT_GROUP_ID

    description = C.PROJECT_DESCRIPTION

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}

val nexusUsername: String? by project
val nexusPassword: String? by project
val signingKey: String? by project
val signingPassword: String? by project
val nexusStagingProfileId: String? by project

subprojects {
    apply {
        plugin("maven-publish")
        plugin("signing")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                val binaryJar = components["java"]

                val sourcesJar by tasks.creating(Jar::class) {
                    archiveClassifier.set("sources")
                    from(sourceSets["main"].allSource)
                }

                val javadocJar: Jar by tasks.creating(Jar::class) {
                    archiveClassifier.set("javadoc")
                    from("$buildDir/javadoc")
                }

                from(binaryJar)
                artifact(sourcesJar)
                artifact(javadocJar)
                pom(BuildConfig.pomAction)
            }
        }
        repositories {
            maven {
                name = "sonatype"
                setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

                credentials {
                    username = nexusUsername
                    password = nexusPassword
                }
            }
        }
    }
    signing {
        sign(publishing.publications)
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
    }
}

nexusPublishing {
    packageGroup.set(group.toString())
    repositories {
        sonatype {
            stagingProfileId.set(nexusStagingProfileId)
            username.set(nexusUsername)
            password.set(nexusPassword)
        }
    }
}
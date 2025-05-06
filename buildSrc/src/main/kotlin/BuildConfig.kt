import org.gradle.api.Action
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.api.publish.maven.MavenPomScm

object BuildConfig {

    val pomAction = Action<MavenPom> {
        name.set(C.PROJECT_NAME)
        description.set(C.PROJECT_DESCRIPTION)
        url.set(C.PROJECT_URL)

        licenses(projectLicenses)
        developers(projectDevelopers)
        scm(projectScm)
    }

    private val projectLicenses = Action<MavenPomLicenseSpec> {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }

    private val projectDevelopers = Action<MavenPomDeveloperSpec> {
        developer {
            id.set("kroegerama")
            name.set("Chris")
            email.set("1519044+kroegerama@users.noreply.github.com")
        }
    }

    private val projectScm = Action<MavenPomScm> {
        url.set(C.PROJECT_URL)
        connection.set("scm:git:https://github.com/kroegerama/openapi-kgen")
        developerConnection.set("scm:git:https://www.github.com/kroegerama")
    }
}
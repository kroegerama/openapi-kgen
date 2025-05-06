plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow)
    implementation(libs.compose.runtime)

    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.okhttp.logging.interceptor)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "app.smartlocker.android"
    compileSdk = 35
    defaultConfig {
        applicationId = providers.gradleProperty("applicationId").getOrElse("app.smartlocker.demo")
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":app"))
    implementation(libs.activity)
}

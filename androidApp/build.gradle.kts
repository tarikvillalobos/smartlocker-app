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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["appName"] = providers.gradleProperty("appName").getOrElse("SmartLocker")
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
dependencies {
    implementation(project(":app"))
    implementation(project(":core"))
    implementation(libs.activity)
    implementation(libs.window)
    implementation(libs.compose.foundation)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.core)
    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.android.compose.test)
    androidTestImplementation(libs.coroutines)
}

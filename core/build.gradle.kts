plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.android.library)
}
kotlin {
    jvm()
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies {
            implementation(libs.serialization)
            implementation(libs.coroutines)
            implementation(libs.datetime)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
android {
    namespace = "app.smartlocker.core"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

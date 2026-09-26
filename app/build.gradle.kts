import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}
kotlin {
    jvm("desktop")
    androidTarget()
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "SmartLocker"
            isStatic = true
        }
    }
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.components.resources)
            implementation(libs.coroutines)
            implementation(libs.serialization)
            implementation(libs.datetime)
            implementation(libs.qrcode)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content)
            implementation(libs.ktor.json)
        }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
                implementation(libs.jna)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
        val desktopTest by getting {
            dependencies { implementation(compose.desktop.uiTestJUnit4)
                implementation(libs.zxing)
                implementation(libs.ktor.client.mock) }
        }
    }
}
android {
    namespace = "app.smartlocker.shared"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}
compose.desktop {
    application {
        mainClass = "app.smartlocker.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SmartLocker"
            packageVersion = "1.0.0"
            modules("java.sql", "java.net.http", "jdk.unsupported")
        }
    }
}

compose.resources {
    packageOfResClass = "app.smartlocker.resources"
    publicResClass = true
}

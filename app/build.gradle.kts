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
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.coroutines.android)
        }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.coroutines.swing)
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
val packageBrand = providers.gradleProperty("brand").getOrElse("smartlocker")
require(packageBrand in setOf("smartlocker", "aurora")) { "Unknown brand: $packageBrand" }
val auroraPackage = packageBrand == "aurora"
val nativeName = if (auroraPackage) "AuroraLockers" else "SmartLocker"
val nativeId = if (auroraPackage) "app.aurora.lockers.demo" else "app.smartlocker.demo"

compose.desktop {
    application {
        mainClass = "app.smartlocker.MainKt"
        args += "--brand=$packageBrand"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = nativeName
            packageVersion = providers.gradleProperty("appVersion").getOrElse("1.0.0")
            description = "Aplicativo de encomendas e armários inteligentes"
            vendor = "SmartLocker"
            licenseFile.set(rootProject.file("docs/PROPRIETARY.txt"))
            modules("java.sql", "java.net.http", "jdk.unsupported", "jdk.crypto.ec")
            macOS {
                bundleID = nativeId
                appCategory = "public.app-category.utilities"
                iconFile.set(rootProject.file(".tools/packaging/icons/$packageBrand.icns"))
            }
            windows {
                iconFile.set(rootProject.file(".tools/packaging/icons/$packageBrand.ico"))
                menu = true
                shortcut = true
                perUserInstall = true
                upgradeUuid = java.util.UUID.nameUUIDFromBytes(nativeId.toByteArray()).toString()
            }
            linux {
                packageName = if (auroraPackage) "aurora-lockers" else "smartlocker"
        }
    }
}

compose.resources {
    packageOfResClass = "app.smartlocker.resources"
    publicResClass = true
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    inputs.property("nativeVaultTests", providers.environmentVariable("SMARTLOCKER_NATIVE_SECURE_TESTS").getOrElse("0"))
}

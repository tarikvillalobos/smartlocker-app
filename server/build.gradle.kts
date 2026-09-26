plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.serialization)
    application
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":core"))
    implementation(libs.serialization)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content)
    implementation(libs.ktor.server.status)
    implementation(libs.ktor.json)
    implementation(libs.sqlite)
    implementation(libs.zxing)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test)
}
application { mainClass.set("app.smartlocker.server.MainKt") }

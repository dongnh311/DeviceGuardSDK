import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":sample:shared"))
    implementation(project(":deviceguard-core"))
    implementation(project(":deviceguard-fingerprint"))
    implementation(project(":deviceguard-rootcheck"))
    implementation(project(":deviceguard-emulator"))
    implementation(project(":deviceguard-integrity"))
    implementation(project(":deviceguard-network"))
    implementation(project(":deviceguard-remote"))
    implementation(project(":deviceguard-surveillance"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(compose.desktop.currentOs)
}

tasks.register<JavaExec>("runHeadless") {
    group = "application"
    description = "Run DeviceGuard headless report on JVM"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.dongnh311.deviceguard.sample.desktop.HeadlessMainKt")
}

compose.desktop {
    application {
        mainClass = "io.github.dongnh311.deviceguard.sample.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DeviceGuard Sample"
            packageVersion = "1.0.0"
        }
    }
}

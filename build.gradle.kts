plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.kover)
}

allprojects {
    group = property("GROUP") as String
    version = property("VERSION_NAME") as String
}

subprojects {
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        toolVersion = rootProject.libs.versions.detekt.get()
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        autoCorrect = false
        parallel = true
    }

    dependencies {
        add(
            "detektPlugins",
            "io.gitlab.arturbosch.detekt:detekt-formatting:${rootProject.libs.versions.detekt.get()}",
        )
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
        android.set(false)
        ignoreFailures.set(false)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        }
        filter {
            exclude { it.file.path.contains("build/") }
        }
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            targets.configureEach {
                compilations.configureEach {
                    compileTaskProvider.configure {
                        compilerOptions {
                            freeCompilerArgs.add("-Xexpect-actual-classes")
                        }
                    }
                }
            }
        }
        apply(plugin = rootProject.libs.plugins.kover.get().pluginId)
    }

    if (name.startsWith("deviceguard-")) {
        apply(plugin = rootProject.libs.plugins.vanniktech.maven.publish.get().pluginId)

        configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral(
                com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL,
                automaticRelease = true,
            )
            signAllPublications()

            coordinates(
                groupId = project.group.toString(),
                artifactId = project.name,
                version = project.version.toString(),
            )

            pom {
                name.set(project.name)
                description.set(
                    project.description ?: "DeviceGuard SDK module: ${project.name}",
                )
                inceptionYear.set("2026")
                url.set("https://github.com/dongnh311/DeviceGuardSDK")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("dongnh311")
                        name.set("DongNH")
                        email.set("hoaidongit5@gmail.com")
                        url.set("https://github.com/dongnh311")
                    }
                }
                scm {
                    url.set("https://github.com/dongnh311/DeviceGuardSDK")
                    connection.set("scm:git:git://github.com/dongnh311/DeviceGuardSDK.git")
                    developerConnection.set("scm:git:ssh://git@github.com/dongnh311/DeviceGuardSDK.git")
                }
            }
        }
    }
}

dependencies {
    kover(project(":deviceguard-core"))
    kover(project(":deviceguard-fingerprint"))
    kover(project(":deviceguard-rootcheck"))
    kover(project(":deviceguard-emulator"))
    kover(project(":deviceguard-integrity"))
    kover(project(":deviceguard-network"))
}

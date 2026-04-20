plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":deviceguard-core"))
        api(project(":deviceguard-fingerprint"))
        api(project(":deviceguard-rootcheck"))
        api(project(":deviceguard-emulator"))
        api(project(":deviceguard-integrity"))
        api(project(":deviceguard-network"))
        api(project(":deviceguard-remote"))
        api(project(":deviceguard-surveillance"))
    }
}

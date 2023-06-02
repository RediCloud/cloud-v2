plugins {
    `base-script`
    id("dev.redicloud.libloader") version "1.6.6"
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.redicloud.dev/releases/")
    }
}
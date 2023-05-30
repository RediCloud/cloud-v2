plugins {
    kotlin("jvm") version "1.8.21"
    id("dev.redicloud.libloader")
}

group = "dev.redicloud"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.redicloud.dev/releases/")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}

kotlin {
    jvmToolchain(8)
}
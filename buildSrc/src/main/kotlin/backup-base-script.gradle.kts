import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

repositories {
    maven("https://repo.redicloud.dev/releases")
    maven("https://repo.redicloud.dev/snapshots")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:${Versions.gson}")
    implementation("dev.redicloud.libloader:libloader-bootstrap:${Versions.libloaderBootstrap}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}")
    implementation("org.redisson:redisson:${Versions.redisson}")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<JavaCompile> {
        options.release.set(8)
        options.encoding = "UTF-8"
    }
}

kotlin {
    jvmToolchain(8)
}
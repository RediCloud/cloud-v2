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
    implementation(BuildDependencies.gson)
    implementation(BuildDependencies.cloudLibloaderBootstrap)
    implementation(BuildDependencies.kotlinxCoroutines)
    implementation(BuildDependencies.redisson)
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
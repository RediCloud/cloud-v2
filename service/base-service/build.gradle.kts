plugins {
    kotlin("jvm") version "1.8.21"
}

group = "dev.redicloud.service.base"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":database"))
    implementation(project(":packets"))
    implementation(project(":utils"))
    implementation(project(":cluster:service-cluster"))


    implementation("org.redisson:redisson:3.21.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}

kotlin {
    jvmToolchain(8)
}
plugins {
    kotlin("jvm") version "1.8.21"
}

group = "dev.redicloud.cluster.service"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":database"))
    implementation(project(":packets"))

    implementation("org.redisson:redisson:3.21.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}
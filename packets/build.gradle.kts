plugins {
    kotlin("jvm") version "1.8.21"
}

group = "dev.redicloud.packets"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":database"))
    implementation(project(":utils"))

    implementation("org.redisson:redisson:3.21.3")
    implementation("com.google.code.gson:gson:2.10.1")
}

kotlin {
    jvmToolchain(8)
}
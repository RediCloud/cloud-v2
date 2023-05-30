plugins {
    kotlin("jvm") version "1.8.21"
}

group = "dev.redicloud.database"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))

    implementation("org.redisson:redisson:3.21.3")
    implementation("com.google.code.gson:gson:2.10.1")
}
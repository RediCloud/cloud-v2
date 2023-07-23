plugins {
    `maven-publish`
}

group = "dev.redicloud"

val publishToRepository by extra(true)

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":logging"))
    dependency("com.google.code.gson:gson:${Versions.gson}")
    testRuntimeOnly("com.google.code.gson:gson:${Versions.gson}")
    implementation("com.google.code.gson:gson:${Versions.gson}")
}
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
    dependency(libs.gson)
    testRuntimeOnly(libs.gson)
    implementation(libs.gson)

    testImplementation(kotlin("test"))
}
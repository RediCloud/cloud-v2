plugins {
    id("redicloud-conventions")
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
    testImplementation(libs.kotlinx.coroutines)
    testRuntimeOnly(project(":logging"))
}
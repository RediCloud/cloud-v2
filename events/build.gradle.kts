plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":packets"))
    compileOnly(project(":logging"))

    dependency(libs.kotlinx.coroutines)
    dependency(libs.gson)
}
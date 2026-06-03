plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.modules"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
    compileOnly(project(":core"))
    compileOnly(project(":commands:command-api"))
    compileOnly(libs.kotlin.reflect)
}
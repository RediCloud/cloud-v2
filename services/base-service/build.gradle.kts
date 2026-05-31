plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.service"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":utils"))
    compileOnly(project(":cache"))
    compileOnly(project(":repositories"))
    compileOnly(project(":events"))
    compileOnly(project(":tasks"))
    compileOnly(project(":console"))
    compileOnly(project(":modules:module-handler"))
    compileOnly(project(":logging"))
    compileOnly(project(":commands:command-api"))

    dependency(libs.kotlinx.coroutines)
    dependency(libs.redisson)
    compileOnly(libs.adventure.api)
    dependency(libs.adventure.serializer.gson)
}
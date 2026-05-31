plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.service"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":cache"))
    compileOnly(project(":utils"))
    compileOnly(project(":repositories"))
    compileOnly(project(":events"))
    compileOnly(project(":tasks"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":services:minecraft-server-service"))
    compileOnly(project(":apis:connector-api"))
    compileOnly(libs.adventure.api)
}
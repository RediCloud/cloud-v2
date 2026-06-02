plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":apis:node-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
    compileOnly(project(":cache"))
    compileOnly(project(":database"))
    compileOnly(project(":logging"))
    compileOnly(project(":console"))
    compileOnly(project(":packets"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":events"))
    compileOnly(project(":file-cluster"))
    compileOnly(project(":repositories"))
    compileOnly(project(":server-factories:remote-server-factory"))

    dependency(libs.jsch)
}
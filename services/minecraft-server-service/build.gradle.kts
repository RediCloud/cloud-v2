plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.service"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":core"))
    compileOnly(project(":server-factories:remote-server-factory"))
    compileOnly(project(":utils"))
    compileOnly(project(":modules:module-handler"))
    compileOnly(project(":repositories"))
    compileOnly(project(":tasks"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":apis:connector-api"))
}
plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":apis:node-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
    compileOnly(project(":core"))
    compileOnly(project(":logging"))
    compileOnly(project(":console"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":file-cluster"))
    compileOnly(project(":repositories"))
}
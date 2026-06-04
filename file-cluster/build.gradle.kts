plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud"

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":logging"))
    compileOnly(project(":core"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":repositories"))

    dependency(libs.sshd)
    dependency(libs.jsch)
    dependency(libs.bcprov)
    dependency(libs.bcpkix)
}
plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud"

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":cache"))
    compileOnly(project(":repositories"))

    dependency(libs.sshd)
    dependency(libs.jsch)
    dependency(libs.bcprov)
    dependency(libs.bcpkix)
}
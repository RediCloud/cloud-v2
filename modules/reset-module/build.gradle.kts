plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.module"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    compileOnly(project(":core"))

    dependency(libs.javalin)
}
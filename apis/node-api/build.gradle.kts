plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.api"

val publishToRepository by extra(true)

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
}
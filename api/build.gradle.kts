plugins {
    `maven-publish`
}

group = "dev.redicloud"

val publishToRepository by extra(true)

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
}
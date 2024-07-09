import java.net.URI

plugins {
    `maven-publish`
}

group = "dev.redicloud.api"

val publishToRepository by extra(true)

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
}
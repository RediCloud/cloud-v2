plugins {
    `maven-publish`
}

group = "dev.redicloud"

val publishToRepository by extra(true)

dependencies {
    shade(libs.logback.core)
    shade(libs.logback.classic)
    shade(libs.testcontainers)
    shade(project(":utils"))
    shade(project(":apis:base-api"))

    testImplementation(libs.logback.core)
    testImplementation(libs.logback.classic)
    testImplementation(libs.testcontainers)
    testImplementation(project(":utils"))
    testImplementation(project(":apis:base-api"))
}
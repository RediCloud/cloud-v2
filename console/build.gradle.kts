plugins {
    `maven-publish`
}

val publishToRepository by extra(true)

group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":logging"))

    testImplementation(project(":commands:command-api"))

    dependency(BuildDependencies.JLINE_CONSOLE)
    dependency(BuildDependencies.JLINE_JANSI)
}
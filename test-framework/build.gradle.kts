plugins {
    `maven-publish`
}

group = "dev.redicloud"

val publishToRepository by extra(true)

dependencies {
    shade(BuildDependencies.LOGBACK_CORE)
    shade(BuildDependencies.LOGBACK_CLASSIC)
    shade(BuildDependencies.DOCKER_TEST_CONTAINERS)
    shade(project(":utils"))
    shade(project(":apis:base-api"))

    testImplementation(BuildDependencies.LOGBACK_CORE)
    testImplementation(BuildDependencies.LOGBACK_CLASSIC)
    testImplementation(BuildDependencies.DOCKER_TEST_CONTAINERS)
    testImplementation(project(":utils"))
    testImplementation(project(":apis:base-api"))
}
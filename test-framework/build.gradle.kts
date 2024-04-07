plugins {
    `maven-publish`
}

group = "dev.redicloud"

val publishToRepository by extra(false)

dependencies {
    implementation(BuildDependencies.LOGBACK_CORE)
    implementation(BuildDependencies.LOGBACK_CLASSIC)
    implementation(BuildDependencies.DOCKER_TEST_CONTAINERS)
    implementation(project(":utils"))
    implementation(project(":apis:base-api"))

    testImplementation(BuildDependencies.LOGBACK_CORE)
    testImplementation(BuildDependencies.LOGBACK_CLASSIC)
    testImplementation(BuildDependencies.DOCKER_TEST_CONTAINERS)
    testImplementation(project(":utils"))
    testImplementation(project(":apis:base-api"))
}
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
    implementation(BuildDependencies.CLOUD_LIBLOADER_BOOTSTRAP)
}
plugins {
    `maven-publish`
}

group = "dev.redicloud"

val publishToRepository by extra(true)

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":logging"))
    dependency(BuildDependencies.gson)
    testRuntimeOnly(BuildDependencies.gson)
    implementation(BuildDependencies.gson)
}
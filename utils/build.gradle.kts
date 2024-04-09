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
    dependency(BuildDependencies.GSON)
    testRuntimeOnly(BuildDependencies.GSON)
    implementation(BuildDependencies.GSON)
}
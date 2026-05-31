plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.module"

dependencies {

    compileOnly(project(":apis:base-api"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    dependency(libs.ktor.client.cio)
    dependency(libs.ktor.client.core) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
    dependency(libs.gson)

    testImplementation(kotlin("test"))
    testImplementation(project(":repositories"))
    testImplementation(project(":utils"))
    testImplementation(project(":logging"))
    testImplementation(libs.kotlinx.coroutines)
}
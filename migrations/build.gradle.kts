plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":core"))
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))

    dependency(libs.kotlinx.coroutines)
}

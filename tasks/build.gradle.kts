plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":logging"))
}
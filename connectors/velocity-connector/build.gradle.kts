plugins {
    id("redicloud-conventions")
    kotlin("kapt")
}

group = "dev.redicloud.connector"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    shade(project(":apis:base-api"))
    shade(project(":cache"))
    shade(project(":services:base-service"))
    shade(project(":services:minecraft-server-service"))
    shade(project(":services:proxy-server-service"))
    shade(project(":repositories"))
    shade(project(":database"))
    shade(project(":utils"))
    shade(project(":events"))
    shade(project(":packets"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":console"))
    shade(project(":tasks"))
    shade(project(":server-factories:remote-server-factory"))
    shade(project(":modules:module-handler"))
    shade(project(":apis:connector-api"))
    shade(libs.libloader.bootstrap)

    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)
}

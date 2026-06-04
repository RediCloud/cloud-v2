plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.connector"

dependencies {
    shade(project(":core"))
    shade(project(":apis:base-api"))
    shade(project(":services:base-service"))
    shade(project(":services:minecraft-server-service"))
    shade(project(":services:proxy-server-service"))
    shade(project(":repositories"))
    shade(project(":utils"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":console"))
    shade(project(":tasks"))
    shade(project(":modules:module-handler"))
    shade(project(":server-factories:remote-server-factory"))
    shade(project(":apis:connector-api"))
    shade(libs.libloader.bootstrap)
    compileOnly(libs.minestom)
    compileOnly(libs.minestom.extensions)
}
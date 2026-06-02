plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.connector"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://libraries.minecraft.net")
}

dependencies {
    shade(project(":cache"))
    shade(project(":apis:base-api"))
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
    shade(project(":modules:module-handler"))
    shade(project(":server-factories:remote-server-factory"))
    shade(project(":apis:connector-api"))
    shade(libs.libloader.bootstrap)

    compileOnly(libs.bungeecord.api)
    dependency(libs.adventure.api)
    dependency(libs.adventure.bungeecord)
}

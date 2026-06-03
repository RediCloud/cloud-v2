import dev.redicloud.libloader.plugin.LibraryLoader

plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.service"

the(LibraryLoader.LibraryLoaderConfig::class).mainClass.set("dev.redicloud.service.node.bootstrap.NodeBootstrapKt")

repositories {
    mavenCentral()
}

dependencies {
    shade(project(":cache"))
    shade(project(":apis:base-api"))
    shade(project(":apis:node-api"))
    shade(project(":services:base-service"))
    shade(project(":repositories"))
    shade(project(":database"))
    shade(project(":utils"))
    shade(project(":events"))
    shade(project(":console"))
    shade(project(":packets"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":tasks"))
    shade(project(":file-cluster"))
    shade(project(":server-factories:node-server-factory"))
    shade(project(":server-factories:remote-server-factory"))
    shade(libs.libloader.bootstrap)
    shade(project(":modules:module-handler"))
    shade(project(":updater"))
    shade(project(":migrations"))
    shade(libs.logback.classic)
    shade(libs.logback.core)
    dependency(libs.adventure.api)
    compileOnly(libs.jline.jansi)
    compileOnly(libs.jsch)
}

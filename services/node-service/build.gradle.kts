import dev.redicloud.libloader.plugin.LibraryLoader

apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.service.node"

the(LibraryLoader.LibraryLoaderConfig::class).mainClass.set("dev.redicloud.service.node.bootstrap.NodeBootstrapKt")

repositories {
    mavenCentral()
}

dependencies {
    shade(project(":services:base-service"))
    shade(project(":repositories:node-repository"))
    shade(project(":repositories:service-repository"))
    shade(project(":repositories:server-repository"))
    shade(project(":repositories:server-version-repository"))
    shade(project(":repositories:server-version-repository"))
    shade(project(":database"))
    shade(project(":utils"))
    shade(project(":events"))
    shade(project(":console"))
    shade(project(":packets"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":tasks"))
    shade("dev.redicloud.libloader:libloader-bootstrap:1.6.7")

    dependency("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
    dependency("org.jline:jline-terminal-jansi:3.23.0")
}
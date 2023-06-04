rootProject.name = "RediCloud-v2"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.redicloud.dev/releases/")
    }
}

include("database")
include("packets")
include("utils")
include("events")
include("console")

include("services:base-service")
findProject(":services:base-service")?.name = "base-service"

include("services:node-service")
findProject(":services:node-service")?.name = "node-service"

include("repositories:service-repository")
findProject(":repositories:service-repository")?.name = "service-repository"

include("repositories:node-repository")
findProject(":repositories:node-repository")?.name = "node-repository"

include("repositories:server-repository")
findProject(":repositories:server-repository")?.name = "server-repository"

include("commands:command-api")
findProject(":commands:command-api")?.name = "command-api"

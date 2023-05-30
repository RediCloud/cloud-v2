rootProject.name = "RediCloud-v2"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.redicloud.dev/releases/")
    }
    plugins {
        id("dev.redicloud.libloader") version ("1.6.6")
    }
}

include("database")
include("packets")
include("utils")

include("service:base-service")
findProject(":service:base-service")?.name = "base-service"

include("cluster:service-cluster")
findProject(":cluster:service-cluster")?.name = "service-cluster"
include("events")

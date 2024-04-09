plugins {
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
    shade(project(":repositories:node-repository"))
    shade(project(":repositories:service-repository"))
    shade(project(":repositories:server-repository"))
    shade(project(":repositories:file-template-repository"))
    shade(project(":repositories:configuration-template-repository"))
    shade(project(":repositories:server-version-repository"))
    shade(project(":repositories:java-version-repository"))
    shade(project(":repositories:player-repository"))
    shade(project(":repositories:cache-repository"))
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
    shade(BuildDependencies.CLOUD_LIBLOADER_BOOTSTRAP)

    compileOnly(BuildDependencies.VELOCITY_API)
    kapt(BuildDependencies.VELOCITY_API)
}

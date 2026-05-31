plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.example.plugin"

repositories {
    /*
    Github redicloud packages:

    maven("https://maven.pkg.github.com/RediCloud/cloud-v2") {
        credentials {
            username = project.findProperty("github_username") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("github_token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
     */

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
}

dependencies {

    /*
    Use the code below to add the cloud api:

    compileOnly("dev.redicloud.api:base-api:<cloud-version>")
     */

    compileOnly(libs.spigot.api)

    // Internal usage, ignore it! You don't need to add this
    compileOnly(project(":apis:base-api"))
    compileOnly(libs.adventure.api)
}
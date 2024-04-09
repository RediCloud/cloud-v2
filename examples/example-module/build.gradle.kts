group = "dev.redicloud.modules"

repositories {
    /*
    Github redicloud packages (more information at https://docs.redicloud.dev/development/repository):

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
    // Internal usage, ignore it
    compileOnly(project(":apis:base-api"))

    /*
    Use the code below to add the cloud api (more information at https://docs.redicloud.dev/development/repository):

    compileOnly("dev.redicloud.api:base-api:<cloud-version>")

    If this should be a node module, you can add also the node-api:
    compileOnly("dev.redicloud.api:node-api:<cloud-version>")
     */

    compileOnly(BuildDependencies.SPIGOT_API)

    // Internal usage, ignore it! You don't need to add this
    compileOnly(project(":apis:base-api"))
}

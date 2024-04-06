group = "dev.redicloud.example.plugin"

repositories {
    // Repo for redicloud artifacts
    maven("https://repo.redicloud.dev/releases")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
}

dependencies {
    // Internal usage, ignore it
    compileOnly(project(":apis:base-api"))

    // External usage, use it
    // compileOnly("dev.redicloud:api:<cloud-version>")

    compileOnly(BuildDependencies.SPIGOT_API)
}
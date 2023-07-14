group = "dev.redicloud.connector.bukkit"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
}

dependencies {
    shade(project(":api"))
    shade(project(":services:base-service"))
    shade(project(":services:minecraft-server-service"))
    shade(project(":repositories:node-repository"))
    shade(project(":repositories:service-repository"))
    shade(project(":repositories:server-repository"))
    shade(project(":repositories:file-template-repository"))
    shade(project(":repositories:configuration-template-repository"))
    shade(project(":repositories:server-version-repository"))
    shade(project(":repositories:java-version-repository"))
    shade(project(":repositories:player-repository"))
    shade(project(":database"))
    shade(project(":utils"))
    shade(project(":events"))
    shade(project(":packets"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":console"))
    shade(project(":tasks"))
    shade("dev.redicloud.libloader:libloader-bootstrap:${Versions.libloaderBootstrap}")

    compileOnly("org.spigotmc:spigot-api:${Versions.minecraftVersion}")
}

tasks.register("buildAndCopy") {
    dependsOn(tasks.named("build"))
    val outputJar = Builds.getOutputFileName(project)
    val original = File(project.buildDir.resolve("libs"), outputJar)
    val outputJarFile = File(project.buildDir.resolve("libs"), "${original.nameWithoutExtension}-local.jar")
    original.renameTo(outputJarFile)
    doLast {
        for (i in 1..Builds.testNodes) {
            val id = if (i in 1..9) "0$i" else i.toString()
            val path = File(Builds.getTestDirPath(project, "node$id"), "storage/connectors")
            project.copy {
                from(outputJarFile)
                into(path)
            }
        }
    }
}

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}
apply(plugin = "com.github.johnrengelman.shadow")

group = "dev.redicloud.connector"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    shade(project(":cache"))
    shade(project(":apis:base-api"))
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
    shade(project(":repositories:cache-repository"))
    shade(project(":database"))
    shade(project(":utils"))
    shade(project(":events"))
    shade(project(":packets"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":console"))
    shade(project(":tasks"))
    shade("dev.redicloud.libloader:libloader-bootstrap:${Versions.libloaderBootstrap}")
    shade(project(":modules:module-handler"))
    shade(project(":server-factories:remote-server-factory"))
    shade(project(":apis:connector-api"))

    compileOnly("org.spigotmc:spigot-api:${Versions.minecraftVersion}")
    shade(project(":connectors:bukkit-legacy"))
}

tasks.register("buildAndCopy") {
    dependsOn(tasks.named("build"))
    doLast {
        val outputJar = Builds.getOutputFileName(project) + ".jar"
        val original = File(project.buildDir.resolve("libs"), outputJar)
        val outputJarFile = File(project.buildDir.resolve("libs"), "${original.nameWithoutExtension}-local.jar")
        original.renameTo(outputJarFile)
        if (original.exists()) {
            original.delete()
        }
        for (i in 1..Builds.testNodes) {
            val id = if (i in 1..9) "0$i" else i.toString()
            val path = File(Builds.getTestDirPath(project, "node$id"), "storage/connectors")
            if (!path.exists()) {
                path.mkdirs()
            }
            val targetJar = File(path, outputJarFile.name)
            if (targetJar.exists()) {
                targetJar.delete()
            }
            project.copy {
                from(outputJarFile)
                into(path)
            }
        }
    }
}

val shadowModJar by tasks.creating(ShadowJar::class) {
    archiveFileName.set(Builds.getOutputFileName(project) + "-shadow.jar")

    relocate("io.netty", "dev.redicloud.netty")
    relocate("com.google.gson", "dev.redicloud.gson")
    relocate("com.google.common", "dev.redicloud.common")

    from(provider { zipTree(tasks.jar.get().archiveFile) })
    destinationDirectory.set(buildDir.resolve("shadowing"))
    archiveVersion.set("")
    manifest.from(provider {
        zipTree(tasks.jar.get().archiveFile)
            .matching { include("META-INF/MANIFEST.MF") }
            .files.first()
    })
}

val copyShadowedJar by tasks.creating {
    dependsOn(shadowModJar)
    doLast {
        shadowModJar.archiveFile.get().asFile.inputStream().use { src ->
            tasks.jar.get().archiveFile.get().asFile.apply { parentFile.mkdirs() }
                .outputStream()
                .use { dst -> src.copyTo(dst) }
        }
    }
}

tasks.build.get().dependsOn(copyShadowedJar)
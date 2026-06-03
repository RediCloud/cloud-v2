
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("redicloud-conventions")
    alias(libs.plugins.shadow)
}

group = "dev.redicloud.connector"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    shade(project(":core"))
    shade(project(":apis:base-api"))
    shade(project(":services:base-service"))
    shade(project(":services:minecraft-server-service"))
    shade(project(":repositories"))
    shade(project(":utils"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":console"))
    shade(project(":tasks"))
    shade(libs.libloader.bootstrap)
    shade(project(":modules:module-handler"))
    shade(project(":server-factories:remote-server-factory"))
    shade(project(":apis:connector-api"))

    compileOnly(libs.spigot.api)
    shade(project(":connectors:bukkit-legacy"))

    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.bukkit)
}

val shadowModJar by tasks.registering(ShadowJar::class) {
    dependsOn(tasks.jar, tasks.named("shadowJar"))
    val v = if (project.version == "unspecified") project.parent?.version ?: "unknown" else project.version
    archiveFileName.set("redicloud-${project.name}-$v-shadow.jar")

    relocate("io.netty", "dev.redicloud.netty")
    relocate("com.google.gson", "dev.redicloud.gson")
    relocate("com.google.common", "dev.redicloud.common")

    from(provider { zipTree(tasks.jar.get().archiveFile) })
    destinationDirectory.set(layout.buildDirectory.dir("shadowing"))
    archiveVersion.set("")
    manifest.from(provider {
        zipTree(tasks.jar.get().archiveFile)
            .matching { include("META-INF/MANIFEST.MF") }
            .files.first()
    })
}

val copyShadowedJar by tasks.registering {
    dependsOn(shadowModJar)
    doLast {
        shadowModJar.get().archiveFile.get().asFile.inputStream().use { src ->
            tasks.jar.get().archiveFile.get().asFile.apply { parentFile.mkdirs() }
                .outputStream()
                .use { dst -> src.copyTo(dst) }
        }
    }
}

tasks.build.get().dependsOn(copyShadowedJar)
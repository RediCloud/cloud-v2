plugins {
    id("redicloud-conventions")
    kotlin("kapt")
}

group = "dev.redicloud.connector"

// Generate BuildConstants.kt so the @Plugin annotation has a compile-time version constant
val generateBuildConstants by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/buildConstants/kotlin")
    val versionString = project.version.toString()
    inputs.property("version", versionString)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("dev/redicloud/connector/velocity")
        dir.mkdirs()
        dir.resolve("BuildConstants.kt").writeText(
            """
            |package dev.redicloud.connector.velocity
            |
            |internal object BuildConstants {
            |    const val VERSION = "$versionString"
            |}
            """.trimMargin()
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generateBuildConstants)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    shade(project(":apis:base-api"))
    shade(project(":core"))
    shade(project(":services:base-service"))
    shade(project(":services:minecraft-server-service"))
    shade(project(":services:proxy-server-service"))
    shade(project(":repositories"))
    shade(project(":utils"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":console"))
    shade(project(":tasks"))
    shade(project(":server-factories:remote-server-factory"))
    shade(project(":modules:module-handler"))
    shade(project(":apis:connector-api"))
    shade(libs.libloader.bootstrap)

    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)
}

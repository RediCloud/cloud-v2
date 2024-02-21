import dev.redicloud.libloader.plugin.LibraryLoader

apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.service"

the(LibraryLoader.LibraryLoaderConfig::class).mainClass.set("dev.redicloud.service.node.bootstrap.NodeBootstrapKt")

repositories {
    mavenCentral()
}

dependencies {
    shade(project(":cache"))
    shade(project(":apis:base-api"))
    shade(project(":apis:node-api"))
    shade(project(":services:base-service"))
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
    shade(project(":console"))
    shade(project(":packets"))
    shade(project(":commands:command-api"))
    shade(project(":logging"))
    shade(project(":tasks"))
    shade(project(":file-cluster"))
    shade(project(":server-factories:node-server-factory"))
    shade(project(":server-factories:remote-server-factory"))
    shade("dev.redicloud.libloader:libloader-bootstrap:${Versions.libloaderBootstrap}")
    shade(project(":repositories:cache-repository"))
    shade(project(":modules:module-handler"))
    shade(project(":updater"))

    compileOnly("org.jline:jline-terminal-jansi:3.23.0")
    compileOnly("com.jcraft:jsch:0.1.55")
}

tasks.register("buildAndCopy") {
    dependsOn(tasks.named("build"))
    val outputJar = Builds.getOutputFileName(project) + ".jar"
    doLast {
        for (i in 1..Builds.testNodes) {
            val id = if (i in 1..9) "0$i" else i.toString()
            val path = Builds.getTestDirPath(project, "node$id")
            project.copy {
                from(project.buildDir.resolve("libs").resolve(outputJar))
                into(path)
            }
        }
    }
}
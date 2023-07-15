group = "dev.redicloud.connector.bungeecord"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    shade(project(":cache"))
    shade(project(":api"))
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
    shade("dev.redicloud.libloader:libloader-bootstrap:${Versions.libloaderBootstrap}")

    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
}

tasks.register("buildAndCopy") {
    dependsOn(tasks.named("build"))
    val outputJar = Builds.getOutputFileName(project) + ".jar"
    val original = File(project.buildDir.resolve("libs"), outputJar)
    val outputJarFile = File(project.buildDir.resolve("libs"), "${original.nameWithoutExtension}-local.jar")
    original.renameTo(outputJarFile)
    doLast {
        for (i in 1..Builds.testNodes) {
            val id = if (i in 1..9) "0$i" else i.toString()
            val path = File(Builds.getTestDirPath(project, "node$id"), "storage/connectors")
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
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
    // compileOnly("dev.redicloud:api:${Versions.cloud}")

    compileOnly(BuildDependencies.spigotApi)
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
            val path = File(Builds.getTestDirPath(project, "node$id"), "storage/templates/example/bukkit/plugins")
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
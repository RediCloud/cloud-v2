group = "dev.redicloud.module"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))

    dependency("io.javalin:javalin:6.1.3")
    compileOnly("org.redisson:redisson:${Versions.redisson}")
}

tasks.register("buildAndCopy") {
    dependsOn(tasks.named("build"))
    doLast {
        val outputJar = Builds.getOutputFileName(project) + ".jar"
        val original = File(project.buildDir.resolve("libs"), outputJar)
        val outputJarFile = File(project.buildDir.resolve("libs"), "${Builds.getOutputModuleFileName(project)}.jar")
        original.renameTo(outputJarFile)
        if (original.exists()) {
            original.delete()
        }
        for (i in 1..Builds.testNodes) {
            val id = if (i in 1..9) "0$i" else i.toString()
            val path = File(Builds.getTestDirPath(project, "node$id"), "storage/modules")
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
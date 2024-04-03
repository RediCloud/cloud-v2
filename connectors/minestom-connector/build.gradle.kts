group = "dev.redicloud.connector"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<JavaCompile> {
        options.release.set(17)
        options.encoding = "UTF-8"

    }
}


dependencies {
    shade(project(":cache"))
    shade(project(":apis:base-api"))
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
    shade(project(":modules:module-handler"))
    shade(project(":server-factories:remote-server-factory"))
    shade(project(":apis:connector-api"))
    shade(BuildDependencies.cloudLibloaderBootstrap)

    compileOnly(BuildDependencies.minestomApi)
    compileOnly(BuildDependencies.minestomExtensions)
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
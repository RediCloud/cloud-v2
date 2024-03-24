import org.gradle.kotlin.dsl.extra

plugins {
    kotlin("jvm")
    id("dev.redicloud.libloader") version Versions.libloader apply false
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "dev.redicloud.libloader")
    apply(plugin = "maven-publish")

    val dependency by configurations.creating
    configurations.compileClasspath.get().extendsFrom(dependency)

    fun DependencyHandlerScope.dependency(dependencyNotation: Any): Dependency? =
        add("dependency", dependencyNotation)

    the(dev.redicloud.libloader.plugin.LibraryLoader.LibraryLoaderConfig::class).configurationName.set("dependency")
    the(dev.redicloud.libloader.plugin.LibraryLoader.LibraryLoaderConfig::class).doBootstrapShade.set(false)

    version = Versions.cloud

    repositories {
        maven("https://repo.redicloud.dev/releases")
        maven("https://repo.redicloud.dev/snapshots")
        maven("https://jitpack.io")
        mavenCentral()
    }

    dependencies {
        compileOnly("com.google.code.gson:gson:${Versions.gson}")
        dependency("dev.redicloud.libloader:libloader-bootstrap:${Versions.libloaderBootstrap}")
        dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}")
        compileOnly("org.redisson:redisson:${Versions.redisson}")
        dependency("com.github.jkcclemens:khttp:${Versions.khttp}")
        dependency("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
        dependency("com.google.inject:guice:${Versions.guice}")
    }

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }

        withType<JavaCompile> {
            options.release.set(8)
            options.encoding = "UTF-8"
        }
    }

    tasks.withType<Jar>() {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            attributes["Main-Class"] = "dev.redicloud.libloader.boot.Bootstrap"
            attributes["Premain-Class"] = "dev.redicloud.libloader.boot.Agent"
            attributes["Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
            attributes["Launcher-Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
        }
        archiveFileName.set(Builds.getOutputFileName(this@allprojects) + ".jar")
    }


    afterEvaluate {
        fun findConfigurationValue(name: String): String? {
            val envValue = System.getenv(name)
            val propValue = findProperty(name)?.toString()
            return envValue ?: propValue
        }
        val publishToRepository = runCatching { extra.get("publishToRepository").toString().toBoolean() }.getOrNull() ?: return@afterEvaluate
        if (!publishToRepository) return@afterEvaluate
        val repositoryUsername = project.findProperty("gpr.user") as String? ?: System.getenv("username")
        val repositoryPassword = project.findProperty("gpr.key") as String? ?: System.getenv("token")
        val repositoryUrl = project.findProperty("gpr.url") as String? ?: ("https://maven.pkg.github.com/" + System.getenv("repository"))
        (extensions["publishing"] as PublishingExtension).apply {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri(repositoryUrl)
                    credentials {
                        username = repositoryUsername
                        password = repositoryPassword
                    }
                }
                publications {
                    register<MavenPublication>("gpr") {
                        from(components["java"])
                    }
                }
            }
        }
    }

}

tasks.register("buildCloudAndCopy") {
    project.allprojects.forEach {
        if (it == it.rootProject) return@forEach
        try {
            val task = it.tasks.named("buildAndCopy")
            dependsOn(task)
            println("Project ${it.name} has ${task.name} task!")
        }catch (_: UnknownDomainObjectException) {
            println("Project ${it.name} has no buildAndCopy task! Use default build task instead.")
            dependsOn(it.tasks.named("build"))
        }
    }
}
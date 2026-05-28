import org.gradle.kotlin.dsl.extra

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.libloader) apply false
    alias(libs.plugins.detekt) apply false
}

val rootLibs = libs

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "dev.redicloud.libloader")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "maven-publish")

    val dependency by configurations.creating
    configurations.compileClasspath.get().extendsFrom(dependency)

    fun DependencyHandlerScope.dependency(dependencyNotation: Any): Dependency? =
        add("dependency", dependencyNotation)

    the(dev.redicloud.libloader.plugin.LibraryLoader.LibraryLoaderConfig::class).configurationName.set("dependency")
    the(dev.redicloud.libloader.plugin.LibraryLoader.LibraryLoaderConfig::class).doBootstrapShade.set(false)

    version = property("cloudVersion") as String

    repositories {
        maven("https://maven.pkg.github.com/RediCloud/gradle-plugins") {
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven("https://jitpack.io")
        mavenCentral()
    }

    dependencies {
        compileOnly(rootLibs.gson)
        dependency(rootLibs.libloader.bootstrap)
        dependency(rootLibs.kotlinx.coroutines)
        dependency(rootLibs.ktor.client.cio) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
        dependency(rootLibs.ktor.client.core) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
        dependency(rootLibs.kotlin.reflect)
        dependency(rootLibs.guice)

        testImplementation(rootLibs.testcontainers)
        testImplementation(rootLibs.gson)
        testImplementation(rootLibs.logback.core)
        testImplementation(rootLibs.logback.classic)
        testImplementation(project(":utils"))
        testImplementation(project(":apis:base-api"))
        testImplementation(project(":database"))
        testImplementation(project(":services:node-service"))
        testImplementation(rootLibs.ktor.client.cio) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
        testImplementation(rootLibs.ktor.client.core) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
    }

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }

        withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            jvmTarget = "1.8"
        }

        withType<JavaCompile> {
            options.release.set(8)
            options.encoding = "UTF-8"
        }
    }

    tasks.withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            attributes["Main-Class"] = "dev.redicloud.libloader.boot.Bootstrap"
            attributes["Premain-Class"] = "dev.redicloud.libloader.boot.Agent"
            attributes["Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
            attributes["Launcher-Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
        }
        val v = if (this@allprojects.version == "unspecified") this@allprojects.parent?.version ?: "unknown" else this@allprojects.version
        archiveFileName.set("redicloud-${this@allprojects.name}-$v.jar")
    }


    afterEvaluate {
        fun findConfigurationValue(name: String): String? {
            val envValue = System.getenv(name)
            val propValue = findProperty(name)?.toString()
            return envValue ?: propValue
        }
        val publishToRepository = runCatching { extra.get("publishToRepository").toString().toBoolean() }.getOrNull() ?: return@afterEvaluate
        if (!publishToRepository) return@afterEvaluate
        val repositoryUsername = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
        val repositoryPassword = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        val repositorySlug = System.getenv("GITHUB_REPOSITORY") ?: System.getenv("repository")
        val repositoryUrl = project.findProperty("gpr.url") as String?
            ?: (repositorySlug?.let { "https://maven.pkg.github.com/$it" } ?: "https://maven.pkg.github.com/RediCloud/cloud-v2")
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
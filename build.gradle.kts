import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.extra

plugins {
    kotlin("jvm")
    id("dev.redicloud.libloader") version BuildDependencies.CLOUD_LIBLOADER_VERSION apply false
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
    extensions.configure(JavaPluginExtension::class.java) {
        withSourcesJar()
    }

    version = BuildDependencies.CLOUD_VERSION

    repositories {
        maven("https://repo.redicloud.dev/releases")
        maven("https://repo.redicloud.dev/snapshots")
        maven("https://jitpack.io")
        mavenCentral()
    }

    dependencies {
        compileOnly(BuildDependencies.GSON)
        dependency(BuildDependencies.CLOUD_LIBLOADER_BOOTSTRAP)
        dependency(BuildDependencies.KOTLINX_COROUTINES)
        dependency(BuildDependencies.KTOR_CLIENT_CIO) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
        dependency(BuildDependencies.KTOR_CLIENT_CORE) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
        dependency(BuildDependencies.KOTLIN_REFLECT)
        dependency(BuildDependencies.GUICE)

        testImplementation(BuildDependencies.DOCKER_TEST_CONTAINERS)
        testImplementation(BuildDependencies.GSON)
        testImplementation(BuildDependencies.LOGBACK_CORE)
        testImplementation(BuildDependencies.LOGBACK_CLASSIC)
        testImplementation(project(":utils"))
        testImplementation(project(":apis:base-api"))
        testImplementation(project(":database"))
        testImplementation(project(":services:node-service"))
        testImplementation(BuildDependencies.KTOR_CLIENT_CIO) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
        testImplementation(BuildDependencies.KTOR_CLIENT_CORE) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
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
        fun findConfigurationValue(vararg names: String): String? {
            names.forEach { name ->
                val propValue = findProperty(name)?.toString()
                if (!propValue.isNullOrBlank()) return propValue
                val envValue = System.getenv(name)
                if (!envValue.isNullOrBlank()) return envValue
            }
            return null
        }
        val publishToRepository = runCatching { extra.get("publishToRepository").toString().toBoolean() }.getOrNull() ?: return@afterEvaluate
        if (!publishToRepository) return@afterEvaluate
        val repositoryUsername = findConfigurationValue("gpr.user", "GPR_USER", "username", "GITHUB_ACTOR")
        val repositoryPassword = findConfigurationValue("gpr.key", "GPR_KEY", "token", "GITHUB_TOKEN")
        val repositoryUrl = findConfigurationValue("gpr.url", "GPR_URL")
            ?: ("https://maven.pkg.github.com/" + findConfigurationValue("repository", "GITHUB_REPOSITORY"))

        extensions.configure(PublishingExtension::class.java) {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri(repositoryUrl)
                    credentials {
                        username = repositoryUsername
                        password = repositoryPassword
                    }
                }
            }
            publications {
                register("gpr", MavenPublication::class.java) {
                    from(components["java"])
                    artifactId = project.name
                    pom {
                        name.set(project.name)
                        description.set("RediCloud API module ${project.path}")
                        url.set("https://github.com/RediCloud/cloud-v2")
                        licenses {
                            license {
                                name.set("Apache License 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        scm {
                            url.set("https://github.com/RediCloud/cloud-v2")
                            connection.set("scm:git:https://github.com/RediCloud/cloud-v2.git")
                            developerConnection.set("scm:git:ssh://git@github.com/RediCloud/cloud-v2.git")
                        }
                    }
                }
            }
        }
    }

}

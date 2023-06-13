import dev.redicloud.libloader.plugin.LibraryLoader
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("dev.redicloud.libloader") version Versions.libloader apply false
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "dev.redicloud.libloader")

    val dependency by configurations.creating
    configurations.compileClasspath.get().extendsFrom(dependency)

    fun DependencyHandlerScope.dependency(dependencyNotation: Any): Dependency? =
        add("dependency", dependencyNotation)

    the(LibraryLoader.LibraryLoaderConfig::class).configurationName.set("dependency")
    the(LibraryLoader.LibraryLoaderConfig::class).doBootstrapShade.set(false)

    version = Versions.cloud

    repositories {
        maven("https://repo.redicloud.dev/releases")
        maven("https://repo.redicloud.dev/snapshots")
        maven("https://jitpack.io")
        mavenCentral()
    }

    dependencies {
        dependency("com.google.code.gson:gson:${Versions.gson}")
        dependency("dev.redicloud.libloader:libloader-bootstrap:${Versions.libloaderBootstrap}")
        dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}")
        dependency("org.redisson:redisson:${Versions.redisson}")
        dependency("com.github.jkcclemens:khttp:${Versions.khttp}")
        dependency("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }

        withType<JavaCompile> {
            options.release.set(8)
            options.encoding = "UTF-8"
        }
    }

    tasks.withType<Jar>() {
        duplicatesStrategy = DuplicatesStrategy.WARN
        manifest {
            attributes["Main-Class"] = "dev.redicloud.libloader.boot.Bootstrap"
            attributes["Premain-Class"] = "dev.redicloud.libloader.boot.Agent"
            attributes["Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
            attributes["Launcher-Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
        }
    }

}
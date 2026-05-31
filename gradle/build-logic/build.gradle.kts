import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.pkg.github.com/RediCloud/gradle-plugins") {
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
                ?: providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.gradleProperty("gpr.key").orNull
                ?: providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${libs.versions.detekt.get()}")
    implementation("dev.redicloud.libloader:libloader:${libs.versions.libloader.get()}")
}

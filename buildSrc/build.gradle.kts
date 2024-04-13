plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    compileOnly(kotlin("gradle-plugin", "1.9.23"))
    runtimeOnly(kotlin("gradle-plugin", "1.9.23"))
}
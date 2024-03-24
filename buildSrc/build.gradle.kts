plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    compileOnly(kotlin("gradle-plugin", "1.8.22"))
    runtimeOnly(kotlin("gradle-plugin", "1.8.22"))
}

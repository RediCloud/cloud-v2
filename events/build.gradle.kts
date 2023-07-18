group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":utils"))
    compileOnly(project(":packets"))
    compileOnly(project(":logging"))

    dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    dependency("com.google.code.gson:gson:${Versions.gson}")
}
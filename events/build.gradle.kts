group = "dev.redicloud.event"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":packets"))
    compileOnly(project(":logging"))

    dependency("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
    dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    dependency("com.google.code.gson:gson:${Versions.gson}")
}
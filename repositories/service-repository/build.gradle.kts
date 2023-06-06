group = "dev.redicloud.repository.service"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))

    dependency("org.redisson:redisson:3.21.3")
    dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}
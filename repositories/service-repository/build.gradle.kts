group = "dev.redicloud.repository"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":logging"))

    dependency("org.redisson:redisson:${Versions.redisson}")
    dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}")
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
}
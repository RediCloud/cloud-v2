group = "dev.redicloud.service.base"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":utils"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:server-repository"))
    compileOnly(project(":events"))


    dependency("org.redisson:redisson:3.21.3")
    dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}
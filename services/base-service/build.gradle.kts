group = "dev.redicloud.service"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":utils"))
    compileOnly(project(":cache"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":repositories:server-repository"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:server-repository"))
    compileOnly(project(":repositories:file-template-repository"))
    compileOnly(project(":repositories:configuration-template-repository"))
    compileOnly(project(":repositories:server-version-repository"))
    compileOnly(project(":repositories:java-version-repository"))
    compileOnly(project(":repositories:player-repository"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":events"))
    compileOnly(project(":tasks"))
    compileOnly(project(":console"))
    compileOnly(project(":modules:module-handler"))
    compileOnly(project(":logging"))
    compileOnly(project(":commands:command-api"))


    dependency("org.redisson:redisson:3.21.3")
    dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}
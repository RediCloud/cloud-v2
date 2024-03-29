group = "dev.redicloud.service"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":database"))
    compileOnly(project(":server-factories:remote-server-factory"))
    compileOnly(project(":packets"))
    compileOnly(project(":utils"))
    compileOnly(project(":cache"))
    compileOnly(project(":modules:module-handler"))
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
    compileOnly(project(":logging"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":apis:connector-api"))
}
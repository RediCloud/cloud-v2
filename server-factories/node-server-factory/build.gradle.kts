group = "dev.redicloud"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":apis:node-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
    compileOnly(project(":cache"))
    compileOnly(project(":database"))
    compileOnly(project(":logging"))
    compileOnly(project(":console"))
    compileOnly(project(":packets"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":events"))
    compileOnly(project(":file-cluster"))
    compileOnly(project(":repositories:server-repository"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:file-template-repository"))
    compileOnly(project(":repositories:configuration-template-repository"))
    compileOnly(project(":repositories:java-version-repository"))
    compileOnly(project(":repositories:server-version-repository"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":server-factories:remote-server-factory"))

    dependency(BuildDependencies.JSCH)
}
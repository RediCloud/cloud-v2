group = "dev.redicloud.repository.server.version"

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
    compileOnly(project(":logging"))
    compileOnly(project(":database"))
    compileOnly(project(":repositories:java-version-repository"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":console"))
}
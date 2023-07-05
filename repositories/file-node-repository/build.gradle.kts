group = "dev.redicloud.repository.filenode"

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":api"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:service-repository"))
}
group = "dev.redicloud.repository.template.file"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
}
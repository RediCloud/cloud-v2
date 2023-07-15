group = "dev.redicloud.repository.template.configuration"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":utils"))
    compileOnly(project(":events"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:server-version-repository"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
}
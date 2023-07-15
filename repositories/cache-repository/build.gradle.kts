group = "dev.redicloud.repository.cache"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":cache"))
    compileOnly(project(":packets"))
}
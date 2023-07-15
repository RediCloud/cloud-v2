group = "dev.redicloud.cache"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":utils"))
}
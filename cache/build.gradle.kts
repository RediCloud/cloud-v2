group = "dev.redicloud.cache"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":logging"))
    compileOnly(project(":database"))
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
}
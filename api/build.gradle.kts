group = "dev.redicloud.api"

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
    compileOnly(project(":tasks"))
    compileOnly(project(":events"))
    compileOnly(project(":packets"))
    compileOnly(project(":database"))
}
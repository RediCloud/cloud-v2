group = "dev.redicloud"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":logging"))
    compileOnly(project(":database"))
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
}
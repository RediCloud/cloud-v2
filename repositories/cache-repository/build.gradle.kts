group = "dev.redicloud.repository"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":cache"))
    compileOnly(project(":packets"))
}
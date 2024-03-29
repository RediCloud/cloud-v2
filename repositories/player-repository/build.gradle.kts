group = "dev.redicloud.repository"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":database"))
    compileOnly(project(":repositories:server-version-repository"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
    compileOnly(project(":packets"))
}
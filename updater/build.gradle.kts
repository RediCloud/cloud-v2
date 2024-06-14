group = "dev.redicloud"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
    compileOnly(project(":database"))
    compileOnly(project(":console"))
    compileOnly(BuildDependencies.REDISSON)
}
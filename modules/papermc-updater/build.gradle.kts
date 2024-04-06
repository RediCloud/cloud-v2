group = "dev.redicloud.module"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    dependency(BuildDependencies.KHTTP)
    dependency(BuildDependencies.GSON)
}
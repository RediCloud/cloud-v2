group = "dev.redicloud.module"

dependencies {

    compileOnly(project(":apis:base-api"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    dependency(BuildDependencies.KTOR_CLIENT_CIO)
    dependency(BuildDependencies.KTOR_CLIENT_CORE)
    dependency(BuildDependencies.GSON)

    testImplementation(kotlin("test"))
    testImplementation(project(":repositories:server-version-repository"))
    testImplementation(project(":utils"))
    testImplementation(BuildDependencies.KOTLINX_COROUTINES)
}
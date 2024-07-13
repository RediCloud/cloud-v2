group = "dev.redicloud.module"

dependencies {

    compileOnly(project(":apis:base-api"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    dependency(BuildDependencies.KTOR_CLIENT_CIO)
    dependency(BuildDependencies.KTOR_CLIENT_CORE) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
    dependency(BuildDependencies.GSON)

    testImplementation(kotlin("test"))
    testImplementation(project(":repositories:server-version-repository"))
    testImplementation(project(":utils"))
    testImplementation(project(":logging"))
    testImplementation(BuildDependencies.KOTLINX_COROUTINES)
}
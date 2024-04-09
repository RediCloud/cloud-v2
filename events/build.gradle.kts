group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":packets"))
    compileOnly(project(":logging"))

    dependency(BuildDependencies.KOTLINX_COROUTINES)
    dependency(BuildDependencies.GSON)
}
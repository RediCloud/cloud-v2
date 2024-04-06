group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":logging"))
    compileOnly(project(":tasks"))

    dependency(BuildDependencies.REDISSON)
    dependency(BuildDependencies.GSON)
}
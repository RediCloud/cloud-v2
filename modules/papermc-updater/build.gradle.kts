group = "dev.redicloud.module"

dependencies {
    testImplementation(kotlin("test"))

    compileOnly(project(":apis:base-api"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    dependency(BuildDependencies.KHTTP)
    dependency(BuildDependencies.GSON)

    testImplementation(BuildDependencies.JUNIT)
    testImplementation(project(":repositories:server-version-repository"))
}

tasks.test {
    useJUnitPlatform()
}
group = "dev.redicloud.repository"

dependencies {
    testImplementation(BuildDependencies.KHTTP)
    implementation(BuildDependencies.KHTTP)
    runtimeOnly(BuildDependencies.KHTTP)
    testRuntimeOnly(BuildDependencies.KHTTP)

    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":tasks"))
    compileOnly(project(":logging"))
    compileOnly(project(":cache"))
    compileOnly(project(":database"))
    compileOnly(project(":repositories:java-version-repository"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":console"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
    compileOnly(project(":packets"))
}
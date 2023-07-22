group = "dev.redicloud.repository"

dependencies {
    testImplementation("com.github.jkcclemens:khttp:${Versions.khttp}")
    implementation("com.github.jkcclemens:khttp:${Versions.khttp}")
    runtimeOnly("com.github.jkcclemens:khttp:${Versions.khttp}")
    testRuntimeOnly("com.github.jkcclemens:khttp:${Versions.khttp}")

    compileOnly(project(":api"))
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
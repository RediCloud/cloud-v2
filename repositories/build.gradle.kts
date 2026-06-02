plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.repository"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":cache"))
    compileOnly(project(":events"))
    compileOnly(project(":logging"))
    compileOnly(project(":tasks"))
    compileOnly(project(":console"))
    compileOnly(libs.redisson)

    dependency(libs.kotlinx.coroutines)
}

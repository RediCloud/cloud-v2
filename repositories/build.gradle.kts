plugins {
    id("redicloud-conventions")
}

group = "dev.redicloud.repository"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":core"))
    compileOnly(project(":logging"))
    compileOnly(project(":tasks"))
    compileOnly(libs.redisson)

    dependency(libs.kotlinx.coroutines)
}

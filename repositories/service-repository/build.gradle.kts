group = "dev.redicloud.repository"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":logging"))

    dependency(libs.kotlinx.coroutines)
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
}
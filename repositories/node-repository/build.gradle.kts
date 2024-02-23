group = "dev.redicloud.repository"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":repositories:server-repository"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":events"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
}
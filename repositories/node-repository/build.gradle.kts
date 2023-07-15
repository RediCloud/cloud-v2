group = "dev.redicloud.repository.node"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":events"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
}
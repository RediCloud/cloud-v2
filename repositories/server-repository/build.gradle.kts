group = "dev.redicloud.repository.server"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
}
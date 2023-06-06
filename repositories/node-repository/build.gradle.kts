group = "dev.redicloud.repository.node"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
}
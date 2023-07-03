group = "dev.redicloud.repository.java.version"
version = "2.0.0-SNAPSHOT"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":logging"))
}
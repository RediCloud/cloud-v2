group = "dev.redicloud.repository.java.version"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))
    compileOnly(project(":logging"))
    compileOnly(project(":repositories:cache-repository"))
    compileOnly(project(":cache"))
    compileOnly(project(":packets"))
}
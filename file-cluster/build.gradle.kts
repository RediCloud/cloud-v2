group = "dev.redicloud"

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":services:base-service"))
    compileOnly(project(":database"))
    compileOnly(project(":packets"))
    compileOnly(project(":cache"))
    compileOnly(project(":repositories:node-repository"))
    compileOnly(project(":repositories:service-repository"))
    compileOnly(project(":repositories:cache-repository"))

    dependency(BuildDependencies.SSHD)
    dependency(BuildDependencies.JSCH)
    dependency(BuildDependencies.BCPROV)
    dependency(BuildDependencies.BCPKIX)
}
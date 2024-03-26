group = "dev.redicloud.modules"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":packets"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":database"))
    compileOnly(BuildDependencies.redisson)
    compileOnly(BuildDependencies.kotlinReflect)
}
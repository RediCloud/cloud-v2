group = "dev.redicloud.modules"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
    compileOnly(project(":events"))
    compileOnly(project(":packets"))
    compileOnly(project(":commands:command-api"))
}
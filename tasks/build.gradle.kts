group = "dev.redicloud.tasks"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":events"))
    compileOnly(project(":logging"))
    compileOnly(project(":packets"))
}
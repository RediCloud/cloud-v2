group = "dev.redicloud.tasks"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":api"))
    compileOnly(project(":logging"))
}
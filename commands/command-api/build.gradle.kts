group = "dev.redicloud.commands.api"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":api"))
}
group = "dev.redicloud"

val publishToRepository by extra(true)

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":api"))
    compileOnly(project(":logging"))
}
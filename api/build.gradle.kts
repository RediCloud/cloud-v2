group = "dev.redicloud"

val publishToRepository by extra("publishToRepository")

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))
}
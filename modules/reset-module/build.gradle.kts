group = "dev.redicloud.module"

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":console"))
    compileOnly(project(":logging"))
    compileOnly(project(":utils"))
    compileOnly(project(":database"))

    dependency("io.javalin:javalin:6.1.6")
}
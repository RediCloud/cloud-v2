import dev.redicloud.libloader.plugin.LibraryLoader

val publishToRepository by extra("publishToRepository")

group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":utils"))
    compileOnly(project(":commands:command-api"))
    compileOnly(project(":logging"))

    testImplementation(project(":commands:command-api"))

    dependency("org.jline:jline-console:3.23.0")
    dependency("org.jline:jline-terminal-jansi:3.23.0")
}
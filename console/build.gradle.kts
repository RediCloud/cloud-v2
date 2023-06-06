import dev.redicloud.libloader.plugin.LibraryLoader

group = "dev.redicloud.console"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":events"))
    compileOnly(project(":logging"))
    compileOnly(project(":commands:command-api"))

    testImplementation(project(":commands:command-api"))
    testImplementation(project(":events"))

    dependency("org.jline:jline-console:3.23.0")
    dependency("org.jline:jline-terminal-jansi:3.23.0")
}
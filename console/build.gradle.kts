import dev.redicloud.libloader.plugin.LibraryLoader

group = "dev.redicloud.console"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":events"))
    compileOnly(project(":commands:command-api"))

    testImplementation(project(":commands:command-api"))
    testImplementation(project(":events"))

    dependency("org.jline:jline-console:3.23.0")
    dependency("org.jline:jline-terminal-jansi:3.23.0")

    dependency("org.slf4j:slf4j-api:2.0.7")
    dependency("ch.qos.logback:logback-core:1.3.7")
    dependency("ch.qos.logback:logback-classic:1.3.7")
}
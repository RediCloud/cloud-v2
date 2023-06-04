plugins {
    `base-script`
}

group = "dev.redicloud.console"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":events"))
    implementation(project(":commands:command-api"))

    implementation("org.jline:jline-console:3.23.0")
    implementation("org.jline:jline-terminal-jansi:3.23.0")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-core:1.3.7")
    implementation("ch.qos.logback:logback-classic:1.3.7")
}
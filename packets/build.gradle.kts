group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":apis:base-api"))
    compileOnly(project(":database"))
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))

    dependency("org.redisson:redisson:${Versions.redisson}")
    dependency("com.google.code.gson:gson:${Versions.gson}")
}
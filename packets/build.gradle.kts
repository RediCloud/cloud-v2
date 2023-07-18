group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":database"))
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))

    dependency("org.redisson:redisson:3.21.3")
    dependency("com.google.code.gson:gson:${Versions.gson}")
}
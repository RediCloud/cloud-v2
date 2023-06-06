group = "dev.redicloud.database"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))
    compileOnly(project(":logging"))

    dependency("org.redisson:redisson:${Versions.redisson}")
    dependency("com.google.code.gson:gson:${Versions.gson}")
    dependency("org.slf4j:slf4j-api:${Versions.slf4j}")
    dependency("org.slf4j:slf4j-simple:${Versions.slf4j}")
}
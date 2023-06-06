group = "dev.redicloud.database"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))

    dependency("org.redisson:redisson:${Versions.redisson}")
    dependency("com.google.code.gson:gson:${Versions.gson}")
}
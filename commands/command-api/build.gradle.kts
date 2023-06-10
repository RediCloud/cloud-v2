group = "dev.redicloud.commands.api"

repositories {
    mavenCentral()
}

dependencies {
    shade(project(":utils"))

    dependency("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
}
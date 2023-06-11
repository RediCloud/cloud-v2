group = "dev.redicloud.commands.api"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":utils"))

    dependency("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
}
plugins {
    `base-script`
}
apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.database"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))

    implementation("org.redisson:redisson:3.21.3")
    implementation("com.google.code.gson:gson:2.10.1")
}
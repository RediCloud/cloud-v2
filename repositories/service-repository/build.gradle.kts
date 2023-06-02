plugins {
    `base-script`
}
apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.repository.service"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":database"))
    implementation(project(":packets"))

    implementation("org.redisson:redisson:3.21.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}
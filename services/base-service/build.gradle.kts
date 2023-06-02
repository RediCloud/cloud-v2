plugins {
    `base-script`
}
apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.service.base"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":database"))
    implementation(project(":packets"))
    implementation(project(":utils"))
    implementation(project(":repositories:service-repository"))
    implementation(project(":repositories:node-repository"))
    implementation(project(":repositories:server-repository"))
    implementation(project(":events"))


    implementation("org.redisson:redisson:3.21.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}

kotlin {
    jvmToolchain(8)
}
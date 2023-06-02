plugins {
    `base-script`
}
apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.repository.server"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":repositories:service-repository"))
    implementation(project(":database"))
}
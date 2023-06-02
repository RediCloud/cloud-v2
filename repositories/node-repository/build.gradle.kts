plugins {
    `base-script`
}
apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.repository.node"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":repositories:service-repository"))
    implementation(project(":utils"))
    implementation(project(":database"))
}
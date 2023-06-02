import dev.redicloud.libloader.plugin.LibraryLoader

plugins {
    `base-script`
}
apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.service.node"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":services:base-service"))
    implementation(project(":repositories:node-repository"))
    implementation(project(":repositories:service-repository"))
    implementation(project(":database"))
    implementation(project(":utils"))

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
}

the(LibraryLoader.LibraryLoaderConfig::class).apply {
    this.mainClass.set("dev.redicloud.service.node.bootstrap.NodeLibLoaderBoostrap")
}
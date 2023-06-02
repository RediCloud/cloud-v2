plugins {
    `base-script`
}
apply(plugin = "dev.redicloud.libloader")

group = "dev.redicloud.event"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":packets"))

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.google.code.gson:gson:2.10.1")
}
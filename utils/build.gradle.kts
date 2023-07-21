group = "dev.redicloud"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":logging"))
    dependency("com.google.code.gson:gson:${Versions.gson}")
    testRuntimeOnly("com.google.code.gson:gson:${Versions.gson}")
    implementation("com.google.code.gson:gson:${Versions.gson}")
}
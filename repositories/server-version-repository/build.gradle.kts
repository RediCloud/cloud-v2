group = "dev.redicloud.repository.server.version"

dependencies {
    shade(project(":utils"))
    shade(project(":database"))

    testImplementation(project(":utils"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinxCoroutines}")
    testImplementation("com.github.jkcclemens:khttp:0.1.0")
    testImplementation("com.google.code.gson:gson:${Versions.gson}")

    dependency("com.github.jkcclemens:khttp:0.1.0")
}
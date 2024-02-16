package dev.redicloud.utils

import java.util.*

val CLOUD_VERSION: String
    get() {return cachedProperties?.getProperty("version", "unknown") ?: "unknown"}
val BUILD_NUMBER: String
    get() {return cachedProperties?.getProperty("build_number", "local") ?: "local"}
val GIT: String
    get() {return cachedProperties?.getProperty("git", "unknown") ?: "unknown"}
val PROJECT_INFO: String
    get() {return cachedProperties?.getProperty("project_info", "CloudV2_Build") ?: "CloudV2_Build"}

val USER_NAME: String = System.getProperty("user.name")

val OS_NAME: String = System.getProperty("os.name")

val JAVA_VERSION: String = System.getProperty("java.version")

private var cachedProperties: Properties? = null
fun loadProperties(classLoader: ClassLoader): Properties? {
    if (cachedProperties != null) return cachedProperties!!
    val url = classLoader.getResource("redicloud-version.properties") ?: return null
    val properties = Properties()
    properties.load(url.openStream())
    cachedProperties = properties
    return properties
}
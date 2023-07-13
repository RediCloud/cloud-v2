package dev.redicloud.utils

import java.util.*

val CLOUD_VERSION: String = properties().getProperty("version", "unknown")
val BUILD_NUMBER: String = properties().getProperty("build_number", "unknown")
val GIT: String = properties().getProperty("git", "unknown")

val USER_NAME: String = System.getProperty("user.name")

val OS_NAME: String = System.getProperty("os.name")

val JAVA_VERSION: String = System.getProperty("java.version")

private var cachedProperties: Properties? = null
private fun properties(): Properties {
    if (cachedProperties != null) return cachedProperties!!
    val url = Thread.currentThread().contextClassLoader.getResource("redicloud-version.properties")!!
    val properties = Properties()
    properties.load(url.openStream())
    cachedProperties = properties
    return properties
}
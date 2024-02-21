package dev.redicloud.utils

import java.util.*

val CLOUD_VERSION: String
    get() {
        return cachedProperties?.getProperty("version", "unknown") ?: "unknown"
    }
val BUILD: String
    get() {
        return cachedProperties?.getProperty("build", "local") ?: "local"
    }
val GIT: String
    get() {
        return cachedProperties?.getProperty("git", "unknown") ?: "unknown"
    }
val BRANCH: String
    get() {
        return cachedProperties?.getProperty("branch", "dev") ?: "dev"
    }

val DEV_BUILD: Boolean
    get() {
        return BRANCH != "master"
    }

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
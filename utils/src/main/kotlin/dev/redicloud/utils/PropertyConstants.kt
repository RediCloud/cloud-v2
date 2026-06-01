package dev.redicloud.utils

import dev.redicloud.utils.version.CloudVersion
import dev.redicloud.utils.version.VersionChannel
import java.util.*

// --- Legacy accessors (backed by properties file) ---

val CLOUD_VERSION: String
    get() = cachedProperties?.getProperty("version", "unknown") ?: "unknown"

val CLOUD_VERSION_FULL: String
    get() = cachedProperties?.getProperty("full-version", "unknown") ?: "unknown"

val CLOUD_VERSION_CHANNEL: String
    get() = cachedProperties?.getProperty("channel", "dev") ?: "dev"

val BUILD: String
    get() = cachedProperties?.getProperty("build", "local") ?: "local"

val GIT: String
    get() = cachedProperties?.getProperty("git", "unknown") ?: "unknown"

val BRANCH: String
    get() = cachedProperties?.getProperty("branch", "dev") ?: "dev"

val DEV_BUILD: Boolean
    get() = CLOUD_VERSION_CHANNEL != VersionChannel.STABLE.label

// --- Typed version model ---

val CLOUD_VERSION_PARSED: CloudVersion?
    get() = CloudVersion.parseOrNull(CLOUD_VERSION_FULL)

// --- System properties ---

val USER_NAME: String = System.getProperty("user.name")

val OS_NAME: String = System.getProperty("os.name")

val JAVA_VERSION: String = System.getProperty("java.version")

// --- Properties loader ---

private var cachedProperties: Properties? = null
fun loadProperties(classLoader: ClassLoader): Properties? {
    if (cachedProperties != null) return cachedProperties!!
    val url = classLoader.getResource("redicloud-version.properties") ?: return null
    val properties = Properties()
    properties.load(url.openStream())
    cachedProperties = properties
    return properties
}

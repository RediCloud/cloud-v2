package dev.redicloud.repository.java.version

import dev.redicloud.api.repositories.java.ICloudJavaVersion
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.utils.OSType
import dev.redicloud.utils.getOperatingSystemType
import dev.redicloud.utils.gson.GsonInterface
import dev.redicloud.utils.service.ServiceId
import java.io.File
import java.util.*

class CloudJavaVersion(
    override val uniqueId: UUID = UUID.randomUUID(),
    override var name: String,
    override var id: Int,
    override val onlineVersion: Boolean = false,
    override val located: MutableMap<UUID, String> = mutableMapOf(),
    override val info: JavaVersionInfo? = null
) : IClusterCacheObject, ICloudJavaVersion {

    override val unknown: Boolean
        get() {
            return id == -1
        }

    override fun isLocated(serviceId: ServiceId) = located.containsKey(serviceId.id) && File(located[serviceId.id]!!).exists()

    override fun autoLocate(): File? {
        return locateAllJavaVersions().filter {
            when (OSType.WINDOWS) {
                getOperatingSystemType() -> File(it, "bin/java.exe")
                else -> File(it, "bin/java")
            }.exists()
        }.filter { file ->
            val byId = file.name.split("-").any { it.lowercase() == id.toString() }
            val byName = file.name.lowercase() == name.lowercase()
            byId || byName
        }.map {
            when (OSType.WINDOWS) {
                getOperatingSystemType() -> File(it, "bin/java.exe")
                else -> File(it, "bin/java")
            }
        }.firstOrNull()
    }

}

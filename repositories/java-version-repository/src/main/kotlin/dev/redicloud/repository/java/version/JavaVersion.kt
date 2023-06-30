package dev.redicloud.repository.java.version

import dev.redicloud.utils.service.ServiceId
import java.io.File
import java.util.*

data class JavaVersion(
    val uniqueId: UUID = UUID.randomUUID(),
    var name: String,
    var id: Int,
    val onlineVersion: Boolean = false,
    val located: MutableMap<ServiceId, String>
) {

    fun isUnknown() = id == -1

    fun isLocated(serviceId: ServiceId) = located.containsKey(serviceId) && File(located[serviceId]!!).exists()

    fun autoLocate(): String? {
        return null //TODO
    }

}

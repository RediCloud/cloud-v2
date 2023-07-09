package dev.redicloud.service.node

import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import dev.redicloud.utils.gson.gson
import java.io.File
import java.util.UUID

data class NodeConfiguration (
    val nodeName: String,
    val uniqueId: UUID,
    val hostAddress: String
) {

    private var cachedServiceId: ServiceId? = null

    companion object {
        fun fromFile(file: File): NodeConfiguration {
            return gson.fromJson(file.readText(), NodeConfiguration::class.java)
        }

    }

    fun toFile(file: File) {
        if (file.exists()) file.delete()
        file.createNewFile()
        file.writeText(gson.toJson(this))
    }

    fun toServiceId(): ServiceId {
        if (cachedServiceId != null) return cachedServiceId!!
        cachedServiceId = ServiceId(uniqueId, ServiceType.NODE)
        return cachedServiceId!!
    }

}
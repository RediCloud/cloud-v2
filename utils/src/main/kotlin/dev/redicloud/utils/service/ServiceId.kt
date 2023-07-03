package dev.redicloud.utils.service

import java.util.UUID

data class ServiceId(val id: UUID, val type: ServiceType) {

    fun toName(): String = "service_${type.name.lowercase().replace("_", "-")}_$id"

    fun toDatabaseIdentifier(): String = "service:${type.name.lowercase()}:$id"

    companion object {
        fun fromString(name: String): ServiceId {
            try {
                val split = name.split("_")
                return ServiceId(UUID.fromString(split[2]), ServiceType.valueOf(split[1].replace("-", "_").uppercase()))
            }catch (e: Exception) {
                throw IllegalArgumentException("Invalid service id: $name", e)
            }
        }
    }
}

fun String.isServiceId(): Boolean {
    return try {
        ServiceId.fromString(this)
        true
    } catch (e: Exception) {
        false
    }
}
package dev.redicloud.api.service

import java.util.UUID

data class ServiceId(val id: UUID, val type: ServiceType) {

    fun toName(): String = "service_${type.name.lowercase().replace("_", "-")}_$id"

    fun toDatabaseIdentifier(): String = "service:${type.name.lowercase()}:$id"

    companion object {
        private const val SERVICE_ID_PARTS = 3

        fun fromString(name: String): ServiceId {
            val split = name.split("_")
            require(split.size >= SERVICE_ID_PARTS) { "Invalid service id: $name" }
            try {
                return ServiceId(UUID.fromString(split[2]), ServiceType.valueOf(split[1].replace("-", "_").uppercase()))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid service id: $name", e)
            }
        }
    }
}

fun String.isServiceId(): Boolean {
    return try {
        ServiceId.fromString(this)
        true
    } catch (_: IllegalArgumentException) {
        false
    }
}

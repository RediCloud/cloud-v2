package dev.redicloud.utils.service

import java.util.UUID

data class ServiceId(val id: UUID, val type: ServiceType) {

    fun toName(): String = "service_${type.name.lowercase()}_$id"

    fun toDatabaseIdentifier(): String = "service:${type.name.lowercase()}:$id"

    companion object {
        fun fromName(name: String): ServiceId {
            val split = name.split("_")
            return ServiceId(UUID.fromString(split[2]), ServiceType.valueOf(split[1].uppercase()))
        }
    }
}
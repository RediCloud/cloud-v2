package dev.redicloud.repository.template.configuration

import dev.redicloud.api.template.configuration.event.ConfigurationTemplateUpdateEvent
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.utils.service.ServiceType
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class ConfigurationTemplateRepository(
    databaseConnection: DatabaseConnection,
    private val eventManager: EventManager,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<ConfigurationTemplate>(
    databaseConnection,
    "configuration-template",
    null,
    ConfigurationTemplate::class,
    5.minutes,
    packetManager,
    ServiceType.NODE
) {
    
    suspend fun getTemplate(uniqueId: UUID): ConfigurationTemplate? {
        return get("$uniqueId")
    }

    suspend fun getTemplate(name: String): ConfigurationTemplate? {
        return getTemplates().firstOrNull { it.name.lowercase() == name.lowercase() }
    }

    suspend fun existsTemplate(uniqueId: UUID): Boolean {
        return exists("$uniqueId")
    }

    suspend fun existsTemplate(name: String): Boolean {
        return getTemplates().any { it.name.lowercase() == name.lowercase() }
    }

    suspend fun createTemplate(configurationTemplate: ConfigurationTemplate) {
        return getHandle(configurationTemplate.uniqueId.toString()).set(configurationTemplate)
    }

    suspend fun updateTemplate(configurationTemplate: ConfigurationTemplate) {
        eventManager.fireEvent(ConfigurationTemplateUpdateEvent(configurationTemplate.uniqueId))
        return set(configurationTemplate.uniqueId.toString(), configurationTemplate)
    }

    suspend fun deleteTemplate(configurationTemplate: ConfigurationTemplate) {
        delete(configurationTemplate.uniqueId.toString())
    }

    suspend fun getTemplates(): List<ConfigurationTemplate> {
        return getAll()
    }

}
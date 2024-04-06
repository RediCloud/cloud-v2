package dev.redicloud.repository.template.configuration

import dev.redicloud.api.events.impl.template.configuration.ConfigurationTemplateUpdateEvent
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplateRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.api.service.ServiceType
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class ConfigurationTemplateRepository(
    databaseConnection: DatabaseConnection,
    private val eventManager: EventManager,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<ICloudConfigurationTemplate, ConfigurationTemplate>(
    databaseConnection,
    "configuration-template",
    ICloudConfigurationTemplate::class,
    ConfigurationTemplate::class,
    5.minutes,
    packetManager,
    ServiceType.NODE
), ICloudConfigurationTemplateRepository {

    override suspend fun getTemplate(uniqueId: UUID): ConfigurationTemplate? {
        return get("$uniqueId")
    }

    override suspend fun getTemplate(name: String): ConfigurationTemplate? {
        return getTemplates().firstOrNull { it.name.lowercase() == name.lowercase() }
    }

    override suspend fun existsTemplate(uniqueId: UUID): Boolean {
        return exists("$uniqueId")
    }

    override suspend fun existsTemplate(name: String): Boolean {
        return getTemplates().any { it.name.lowercase() == name.lowercase() }
    }

    override suspend fun createTemplate(configurationTemplate: ICloudConfigurationTemplate): ConfigurationTemplate {
        return set(configurationTemplate.uniqueId.toString(), configurationTemplate)
    }

    override suspend fun updateTemplate(configurationTemplate: ICloudConfigurationTemplate): ConfigurationTemplate {
        eventManager.fireEvent(ConfigurationTemplateUpdateEvent(configurationTemplate.uniqueId))
        return set(configurationTemplate.uniqueId.toString(), configurationTemplate)
    }

    override suspend fun deleteTemplate(configurationTemplate: ICloudConfigurationTemplate): Boolean {
        return delete(configurationTemplate.uniqueId.toString())
    }

    override suspend fun getTemplates(): List<ConfigurationTemplate> {
        return getAll()
    }

}
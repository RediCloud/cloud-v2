package dev.redicloud.repository.template.configuration

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import java.util.UUID

class ConfigurationTemplateRepository(databaseConnection: DatabaseConnection) :
    DatabaseBucketRepository<ConfigurationTemplate>(databaseConnection, "configuration-template") {

    suspend fun getTemplate(uniqueId: UUID): ConfigurationTemplate? {
        return get("$uniqueId")
    }

    suspend fun getTemplate(name: String): ConfigurationTemplate? {
        return getTemplates().firstOrNull { it.name == name }
    }

    suspend fun existsTemplate(uniqueId: UUID): Boolean {
        return exists("$uniqueId")
    }

    suspend fun existsTemplate(name: String): Boolean {
        return getTemplates().any { it.name == name }
    }

    suspend fun createTemplate(configurationTemplate: ConfigurationTemplate) {
        return getHandle(configurationTemplate.uniqueId.toString()).set(configurationTemplate)
    }

    suspend fun updateTemplate(configurationTemplate: ConfigurationTemplate) {
        return set(configurationTemplate.uniqueId.toString(), configurationTemplate)
    }

    suspend fun deleteTemplate(configurationTemplate: ConfigurationTemplate) {
        delete(configurationTemplate.uniqueId.toString())
    }

    suspend fun getTemplates(): List<ConfigurationTemplate> {
        return getAll()
    }

}
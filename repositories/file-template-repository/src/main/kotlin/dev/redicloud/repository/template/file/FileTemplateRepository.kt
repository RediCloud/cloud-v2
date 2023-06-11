package dev.redicloud.repository.template.file

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository

class FileTemplateRepository(databaseConnection: DatabaseConnection) :
    DatabaseBucketRepository<FileTemplate>(databaseConnection, "file-template") {

    suspend fun getTemplate(name: String, prefix: String): FileTemplate? {
        return getHandle("${prefix.lowercase()}:${name.lowercase()}").get()
    }

    suspend fun existsTemplate(name: String, prefix: String): Boolean {
        return getHandle("${prefix.lowercase()}:${name.lowercase()}").isExists
    }

    suspend fun updateTemplate(template: FileTemplate, oldPrefix: String? = null, oldName: String? = null): FileTemplate {
        var identifier = oldPrefix ?: template.prefix
        identifier += oldName ?: template.name
        identifier = identifier.lowercase()
        getHandle(identifier).set(template)
        return template
    }

    suspend fun createTemplate(template: FileTemplate): FileTemplate {
        getHandle("${template.prefix.lowercase()}:${template.name.lowercase()}").set(template)
        return template
    }

    suspend fun getTemplates(): List<FileTemplate> = getAll()

}
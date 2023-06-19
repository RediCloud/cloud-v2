package dev.redicloud.repository.template.file

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.node.NodeRepository
import java.util.UUID

class FileTemplateRepository(
    databaseConnection: DatabaseConnection,
    val nodeRepository: NodeRepository
) :
    DatabaseBucketRepository<FileTemplate>(databaseConnection, "file-template") {

    suspend fun getTemplate(uniqueId: UUID): FileTemplate? {
        return getHandle(uniqueId.toString()).get()
    }

    suspend fun getTemplate(name: String, prefix: String): FileTemplate? {
        return getTemplates().firstOrNull { it.name.lowercase() == name.lowercase() && it.prefix.lowercase() == prefix.lowercase() }
    }

    suspend fun existsTemplate(uniqueId: UUID, prefix: String): Boolean {
        return getHandle(uniqueId.toString()).isExists
    }

    suspend fun existsTemplate(name: String, prefix: String): Boolean {
        return getTemplates().any { it.name.lowercase() == name.lowercase() && it.prefix.lowercase() == prefix.lowercase() }
    }

    suspend fun updateTemplate(template: FileTemplate): FileTemplate {
        getHandle(template.uniqueId.toString()).set(template)
        return template
    }

    suspend fun createTemplate(template: FileTemplate): FileTemplate {
        getHandle(template.uniqueId.toString()).set(template)
        return template
    }

    suspend fun getTemplates(): List<FileTemplate> = getAll()

}
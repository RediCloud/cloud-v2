package dev.redicloud.repository.template.file

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.node.NodeRepository
import java.util.UUID

class FileTemplateRepository(databaseConnection: DatabaseConnection, val packetManager: PacketManager, val nodeRepository: NodeRepository) :
    DatabaseBucketRepository<FileTemplate>(databaseConnection, "file-template") {

    suspend fun getTemplate(uniqueId: UUID): FileTemplate? {
        return getHandle("$uniqueId").get()
    }

    suspend fun getTemplate(name: String, prefix: String): FileTemplate? {
        return getTemplates().firstOrNull { it.name == name && it.prefix == prefix }
    }

    suspend fun existsTemplate(uniqueId: UUID, prefix: String): Boolean {
        return getHandle("$uniqueId").isExists
    }

    suspend fun existsTemplate(name: String, prefix: String): Boolean {
        return getTemplates().any { it.name == name && it.prefix == prefix }
    }

    suspend fun updateTemplate(template: FileTemplate): FileTemplate {
        getHandle("${template.uniqueId}").set(template)
        return template
    }

    suspend fun createTemplate(template: FileTemplate): FileTemplate {
        getHandle("${template.uniqueId}").set(template)
        return template
    }

    suspend fun getTemplates(): List<FileTemplate> = getAll()

}
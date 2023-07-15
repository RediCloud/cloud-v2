package dev.redicloud.service.base.repository

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.utils.service.ServiceId

class BaseFileTemplateRepository(
    databaseConnection: DatabaseConnection,
    nodeRepository: NodeRepository,
    packetManager: PacketManager
) : AbstractFileTemplateRepository(databaseConnection, nodeRepository, packetManager) {

    override suspend fun updateTemplate(template: FileTemplate): FileTemplate {
        TODO("Not yet implemented")
    }

    override suspend fun pushTemplates(serviceId: ServiceId) {
        TODO("Not yet implemented")
    }

}
package dev.redicloud.service.node

import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.node.repository.connect
import dev.redicloud.utils.ServiceId
import dev.redicloud.utils.ServiceType

class NodeService(databaseConfiguration: DatabaseConfiguration, val configuration: NodeConfiguration)
    : BaseService(databaseConfiguration, ServiceId(configuration.uniqueId, ServiceType.NODE)) {

    init {
        nodeRepository.connect()
    }

}
package dev.redicloud.service.node

import dev.redicloud.database.config.DatabaseConfig
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.node.repository.connect
import dev.redicloud.utils.ServiceId

class NodeService(databaseConfig: DatabaseConfig, serviceId: ServiceId) : BaseService(databaseConfig, serviceId) {

    init {
        nodeRepository.connect()
    }

}
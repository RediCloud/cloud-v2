package dev.redicloud.repository.node

import dev.redicloud.api.repositories.service.node.ICloudNode
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSessions
import dev.redicloud.utils.service.ServiceId

class CloudNode(
    serviceId: ServiceId,
    name: String,
    serviceSessions: ServiceSessions,
    override val hostedServers: MutableList<ServiceId>,
    override var master: Boolean,
    override var currentMemoryUsage: Long,
    override var maxMemory: Long
) : CloudService(serviceId, name, serviceSessions), ICloudNode, IClusterCacheObject
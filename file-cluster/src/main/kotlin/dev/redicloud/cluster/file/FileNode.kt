package dev.redicloud.cluster.file

import dev.redicloud.api.service.file.IFileNode
import dev.redicloud.cache.IClusterCacheObject
import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSessions
import dev.redicloud.api.service.ServiceId

class FileNode(
    serviceId: ServiceId,
    override var port: Int = -1,
    override var hostname: String,
    var username: String,
    internal var password: String,
    override val nodeInternal: Boolean,
    override val cloudPath: String
) : CloudService(serviceId, serviceId.toName(), ServiceSessions()), IFileNode, IClusterCacheObject
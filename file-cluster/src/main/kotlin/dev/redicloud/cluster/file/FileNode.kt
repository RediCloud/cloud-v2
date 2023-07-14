package dev.redicloud.cluster.file

import dev.redicloud.repository.service.CloudService
import dev.redicloud.repository.service.ServiceSessions
import dev.redicloud.utils.service.ServiceId
import java.security.PrivateKey
import java.security.PublicKey

class FileNode(
    serviceId: ServiceId,
    var port: Int = -1,
    var hostname: String,
    var username: String,
    internal var password: String,
    val nodeInternal: Boolean,
    val cloudPath: String
) : CloudService(serviceId, serviceId.toName(), ServiceSessions())
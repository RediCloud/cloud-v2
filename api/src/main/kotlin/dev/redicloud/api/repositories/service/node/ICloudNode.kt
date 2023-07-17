package dev.redicloud.api.repositories.service.node

import dev.redicloud.api.repositories.service.ICloudService
import dev.redicloud.utils.service.ServiceId

interface ICloudNode : ICloudService {

    val hostedServers: MutableList<ServiceId>
    val master: Boolean
    val currentMemoryUsage: Long
    var maxMemory: Long

}
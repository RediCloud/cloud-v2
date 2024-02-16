package dev.redicloud.api.service.node

import dev.redicloud.api.service.ICloudService
import dev.redicloud.api.service.ServiceId

interface ICloudNode : ICloudService {

    val hostedServers: MutableList<ServiceId>
    val master: Boolean
    val currentMemoryUsage: Long
    var maxMemory: Long

}
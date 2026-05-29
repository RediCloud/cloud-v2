package dev.redicloud.repository.player

import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.service.ServiceId
import dev.redicloud.cache.IClusterCacheObject
import java.util.UUID

class CloudPlayer(
    override val uniqueId: UUID,
    override var name: String,
    override var serverId: ServiceId?,
    override var proxyId: ServiceId?,
    override var lastDisconnect: Long?,
    override var lastConnect: Long?,
    override var firstConnect: Long?,
    override var protocolId: Int,
    override var connected: Boolean = false
) : IClusterCacheObject, ICloudPlayer

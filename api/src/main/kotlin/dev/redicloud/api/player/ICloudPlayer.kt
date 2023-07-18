package dev.redicloud.api.player

import dev.redicloud.api.service.ServiceId
import java.util.*

interface ICloudPlayer {

    val uniqueId: UUID
    val name: String
    val serverId: ServiceId?
    val proxyId: ServiceId?
    val lastDisconnect: Long?
    val lastConnect: Long?
    val firstConnect: Long?
    val protocolId: Int
    val connected: Boolean

}
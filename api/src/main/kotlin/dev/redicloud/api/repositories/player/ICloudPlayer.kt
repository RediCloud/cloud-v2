package dev.redicloud.api.repositories.player

import dev.redicloud.utils.service.ServiceId
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
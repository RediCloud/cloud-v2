package dev.redicloud.service.base.packets.listener

import dev.redicloud.api.packets.PacketListener
import dev.redicloud.logging.LogManager
import dev.redicloud.service.base.BaseService
import dev.redicloud.service.base.packets.service.CloudServiceShutdownPacket
import dev.redicloud.service.base.packets.service.CloudServiceShutdownResponse

private val logger = LogManager.logger(CloudServiceShutdownPacketListener::class)
class CloudServiceShutdownPacketListener(baseService: BaseService) : PacketListener<CloudServiceShutdownPacket>(
    CloudServiceShutdownPacket::class,
    { packet ->
        logger.fine("Received shutdown packet from ${packet.sender}")
        packet.respond(CloudServiceShutdownResponse())
        baseService.platformShutdown()
    }
)

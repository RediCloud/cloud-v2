package dev.redicloud.service.base.packets.listener

import dev.redicloud.api.service.packets.CloudServiceShutdownPacket
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketListener
import dev.redicloud.service.base.BaseService

private val logger = LogManager.logger(CloudServiceShutdownPacketListener::class)
class CloudServiceShutdownPacketListener(baseService: BaseService) : PacketListener<CloudServiceShutdownPacket>(CloudServiceShutdownPacket::class, { packet ->
    if (packet.sender == baseService.serviceId) {
        logger.fine("Received shutdown packet from ${packet.sender}")
        baseService.shutdown()
    }
})
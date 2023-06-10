package dev.redicloud.service.base.packets

import dev.redicloud.packets.AbstractPacket

class ServicePingResponse(val receivedPingTime: Long) : AbstractPacket()
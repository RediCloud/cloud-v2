package dev.redicloud.service.base.packets

import dev.redicloud.api.packets.AbstractPacket

class ServicePingResponse(val receivedPingTime: Long) : AbstractPacket()
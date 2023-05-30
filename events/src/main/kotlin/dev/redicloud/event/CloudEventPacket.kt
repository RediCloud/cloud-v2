package dev.redicloud.event

import dev.redicloud.packets.AbstractPacket

class CloudEventPacket(val event: CloudEvent) : AbstractPacket()
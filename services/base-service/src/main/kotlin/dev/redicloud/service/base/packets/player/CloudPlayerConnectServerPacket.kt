package dev.redicloud.service.base.packets.player

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.ICloudServer
import java.util.UUID

class CloudPlayerConnectServerPacket(
    uniqueId: UUID,
    val serviceId: ServiceId
) : CloudPlayerPacket(uniqueId) {

    constructor(uniqueId: UUID, cloudServer: ICloudServer) : this(
        uniqueId,
        cloudServer.serviceId
    )

}
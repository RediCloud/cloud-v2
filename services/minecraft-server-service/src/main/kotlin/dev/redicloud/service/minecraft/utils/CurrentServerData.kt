package dev.redicloud.service.minecraft.utils

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.utils.ICurrentServerData
import java.util.UUID

class CurrentServerData(
    override val serviceId: ServiceId,
    override var name: String,
    override var id: Int,
    override var maxPlayers: Int,
    override var connectedPlayers: MutableList<UUID>,
    override var state: CloudServerState,
    override var configurationTemplateName: String,
    override val serverVersionName: String
) : ICurrentServerData
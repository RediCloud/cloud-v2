package dev.redicloud.service.minecraft.tasks

import dev.redicloud.api.provider.IServerPlayerProvider
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.tasks.CloudTask
import dev.redicloud.api.service.ServiceId
import dev.redicloud.service.minecraft.utils.CurrentServerData

class CloudServerInfoTask(
    private val serviceId: ServiceId,
    private val serverRepository: ServerRepository,
    private val playerProvider: IServerPlayerProvider,
    private val currentServerData: CurrentServerData
) : CloudTask() {

    override suspend fun execute(): Boolean {
        val cloudServer = serverRepository.getServer<CloudServer>(serviceId) ?: return false

        currentServerData.name = cloudServer.name
        currentServerData.id = cloudServer.id
        currentServerData.maxPlayers = cloudServer.maxPlayers
        currentServerData.connectedPlayers = cloudServer.connectedPlayers
        currentServerData.state = cloudServer.state
        currentServerData.configurationTemplateName = cloudServer.configurationTemplate.name

        var update = false
        if (cloudServer.maxPlayers != playerProvider.getMaxPlayerCount()) update = true
        cloudServer.maxPlayers = playerProvider.getMaxPlayerCount()
        val players = playerProvider.getConnectedPlayers()
        if (cloudServer.connectedPlayers.toList() != players) update = true
        cloudServer.connectedPlayers = players.toMutableList()
        if (update) serverRepository.updateServer(cloudServer)
        return false
    }

}
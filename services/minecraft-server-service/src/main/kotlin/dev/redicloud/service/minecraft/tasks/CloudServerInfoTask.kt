package dev.redicloud.service.minecraft.tasks

import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import dev.redicloud.tasks.CloudTask

class CloudServerInfoTask(
    private val serverRepository: ServerRepository,
    private val playerCountProvider: IServerPlayerProvider
) : CloudTask() {

    override suspend fun execute(): Boolean {
        val cloudServer = serverRepository.getServer<CloudServer>(serverRepository.serviceId) ?: return false
        var update = false
        if (cloudServer.maxPlayers != playerCountProvider.getMaxPlayerCount()) update = true
        cloudServer.maxPlayers = playerCountProvider.getMaxPlayerCount()
        val players = playerCountProvider.getConnectedPlayers()
        if (cloudServer.connectedPlayers.toList() != players) update = true
        cloudServer.connectedPlayers = players.toMutableList()
        if (update) serverRepository.updateServer(cloudServer)
        return false
    }

}
package dev.redicloud.service.node.tasks.player

import dev.redicloud.api.service.ServiceId
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.player.PlayerRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.tasks.CloudTask

class PlayerProxyConnectionStateTask(
    private val playerRepository: PlayerRepository,
    private val serverRepository: ServerRepository,
    private val nodeRepository: NodeRepository,
    private val hostNodeId: ServiceId
) : CloudTask() {

    override suspend fun execute(): Boolean {
        val node = nodeRepository.getNode(hostNodeId)
        if (node?.master != true) return false
        playerRepository.getConnectedPlayers().forEach {
            var update = false
            if (it.proxyId != null) {
                val proxy = serverRepository.getProxyServer(it.proxyId!!)
                if (proxy == null || !proxy.connected) {
                    it.proxyId = null
                    it.serverId = null
                    it.connected = false
                    it.lastDisconnect = System.currentTimeMillis()
                    update = true
                }
            }
            if (it.serverId != null) {
                val server = serverRepository.getMinecraftServer(it.serverId!!)
                if (server == null || !server.connected) {
                    it.serverId = null
                    if (it.proxyId == null) {
                        it.connected = false
                        it.lastDisconnect = System.currentTimeMillis()
                    }
                    update = true
                }
            }
            if (update) {
                playerRepository.updatePlayer(it)
            }
        }
        return false
    }

}
package dev.redicloud.example.plugin

import com.google.inject.Inject
import com.google.inject.name.Named
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.api.utils.injectCloudApi
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class ExamplePlugin : JavaPlugin(), CloudInjectable {

    @Inject
    @Named("this")
    private lateinit var serviceId: ServiceId

    @Inject
    @Named("host")
    private lateinit var hostServiceId: ServiceId

    @Inject
    private lateinit var playerRepository: ICloudPlayerRepository

    @Inject
    private lateinit var serverRepository: ICloudServerRepository

    @Inject
    private lateinit var nodeRepository: ICloudNodeRepository

    private val logger = Bukkit.getLogger()

    override fun onEnable() = runBlocking {
        injectCloudApi()
        logger.info("<================================>")
        logger.info("Example redicloud plugin enabled")
        val service = serverRepository.getServer<ICloudServer>(serviceId)!!
        logger.info("Server: ${service.identifyName()}")
        val node = nodeRepository.getNode(hostServiceId)!!
        logger.info("Node: ${node.identifyName()}")
        logger.info("Started services: ${serverRepository.getConnectedServers().joinToString(", ") { it.name }}")
        logger.info("Players: ${playerRepository.getConnectedPlayers().joinToString(", ") { it.name }}")
        logger.info("<================================>")
    }

}
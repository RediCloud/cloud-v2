package dev.redicloud.service.node.tasks.metrics

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.java.version.getJavaVersion
import dev.redicloud.repository.player.PlayerRepository
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.service.base.utils.ClusterConfiguration
import dev.redicloud.tasks.CloudTask
import dev.redicloud.utils.*
import dev.redicloud.utils.gson.gson
import dev.redicloud.api.service.ServiceId
import java.util.*

class MetricsTask(
    private val clusterConfiguration: ClusterConfiguration,
    private val nodeId: ServiceId,
    private val playerRepository: PlayerRepository,
    private val serverRepository: ServerRepository
) : CloudTask() {

    companion object {
        private val logger = LogManager.logger(MetricsTask::class)
    }

    override suspend fun execute(): Boolean {
        if (System.getProperty("dev.redicloud.metrics", "true").lowercase() == "false") return true
        try {
            val url = "https://api.redicloud.dev/metrics/$PROJECT_INFO"
            if (khttp.post(url, data = "ping").statusCode != 200) {
                logger.fine("Failed to send metrics to redicloud api: invalid response to ping")
                return false
            }
            val metric = Metric(
                UUID.fromString(clusterConfiguration.get("id")),
                nodeId.id,
                BUILD_NUMBER,
                GIT,
                CLOUD_VERSION,
                getJavaVersion().id,
                playerRepository.getConnectedPlayers().size,
                serverRepository.getConnectedServers().map {
                    it.serviceId.id to it.hostNodeId.id
                }.toMap()
            )
            val response = khttp.post(url, data = gson.toJson(metric))
            if (response.statusCode == 200) {
                logger.fine("Sent metrics to redicloud api")
            }else {
                logger.fine("Failed to send metrics to redicloud api: ${response.statusCode}")
            }
        }catch (e: Exception) {
            logger.fine("Failed to send metrics to redicloud api", e)
        }
        return false
    }

}

data class Metric(
    val clusterId: UUID,
    val nodeId: UUID,
    val buildNumber: String,
    val git: String,
    val version: String,
    val javaLevel: Int,
    val connectedPlayers: Int,
    val registeredServers: Map<UUID, UUID>
)
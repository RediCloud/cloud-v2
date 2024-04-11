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
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
        if (System.getenv().containsKey("REDICLOUD_TESTING") && System.getenv("REDICLOUD_TESTING") == "true") return true
        if (System.getProperty("dev.redicloud.metrics", "true").lowercase() == "false") return true
        try {
            val url = "https://api.redicloud.dev/v2/metrics"
            val pingResponse = httpClient.post {
                url(url)
                setBody("ping")
            }
            if (!pingResponse.status.isSuccess()) {
                logger.fine("Failed to send metrics to redicloud api: invalid response to ping")
                return false
            }
            val metric = Metric(
                UUID.fromString(clusterConfiguration.get("id")),
                nodeId.id,
                BUILD,
                BRANCH,
                GIT,
                CLOUD_VERSION,
                getJavaVersion().id,
                playerRepository.getConnectedPlayers().size,
                serverRepository.getConnectedServers().map {
                    it.serviceId.id to it.hostNodeId.id
                }.toMap()
            )
            val response = httpClient.post {
                url(url)
                setBody(gson.toJson(metric))
            }
            if (response.status.isSuccess()) {
                logger.fine("Sent metrics to redicloud api")
            }else {
                logger.fine("Failed to send metrics to redicloud api: ${response.status.value} ${response.bodyAsText()}")
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
    val build: String,
    val branch: String,
    val git: String,
    val version: String,
    val javaLevel: Int,
    val connectedPlayers: Int,
    val registeredServers: Map<UUID, UUID>
)
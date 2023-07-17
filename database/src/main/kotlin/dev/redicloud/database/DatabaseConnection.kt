package dev.redicloud.database

import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.database.repository.DatabaseRepository
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.BaseCodec
import org.redisson.config.Config

class DatabaseConnection(
    config: DatabaseConfiguration,
    val serviceId: ServiceId,
    val codec: BaseCodec = GsonCodec(),
    var connectionPoolSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 64/2 else 64,
    var connectionMinimumIdleSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 24/2 else 24,
    var subscriptionConnectionPoolSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 50/2 else 50,
    var subscriptionConnectionMinimumIdleSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 2/2 else 2
) {

    companion object {
        private val LOGGER = LogManager.logger(DatabaseConnection::class)
    }

    private val redissonConfig = Config()
    private var client: RedissonClient? = null
    private val repositories = mutableListOf<DatabaseRepository<*>>()

    init {
        redissonConfig
            .setCodec(codec)
        if (config.isCluster()) {
            val clusterConfig = redissonConfig.useClusterServers()
                .setClientName(serviceId.toName())
                .setPassword(config.password)
            config.nodes.forEach { node ->
                clusterConfig.addNodeAddress(node.toConnectionString())
            }
            clusterConfig.setSlaveConnectionPoolSize(connectionPoolSize)
                .setMasterConnectionPoolSize(connectionPoolSize)
                .setMasterConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
        }else {
            redissonConfig.useSingleServer()
                .setAddress(config.nodes.first().toConnectionString())
                .setDatabase(config.databaseId)
                .setClientName(serviceId.toName())
                .setPassword(config.password)
                .setConnectionPoolSize(connectionPoolSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
        }
    }

    suspend fun connect() {
        client = Redisson.create(redissonConfig)
        LOGGER.fine("Successfully connected to redis")
    }

    fun disconnect() {
        if (isConnected()) client!!.shutdown()
        LOGGER.fine("Successfully disconnected from redis")
    }

    fun isConnected(): Boolean {
        if (client == null) return false
        return !client!!.isShuttingDown
    }

    fun getClient(): RedissonClient = client!!

}
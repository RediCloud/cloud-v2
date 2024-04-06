package dev.redicloud.database

import dev.redicloud.api.database.IDatabaseConnection
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.database.repository.DatabaseRepository
import dev.redicloud.logging.LogManager
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.BaseCodec
import org.redisson.config.Config

class DatabaseConnection(
    config: DatabaseConfiguration,
    val serviceId: ServiceId,
    codec: BaseCodec = GsonCodec(),
    connectionPoolSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 64/2 else 64,
    connectionMinimumIdleSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 24/2 else 24,
    subscriptionConnectionPoolSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 50/2 else 50,
    subscriptionConnectionMinimumIdleSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 2/2 else 2
) : IDatabaseConnection {

    companion object {
        private val LOGGER = LogManager.logger(DatabaseConnection::class)
    }

    private val redissonConfig = Config()
    private var client: RedissonClient? = null

    init {
        redissonConfig
            .setCodec(codec)
        if (config.isCluster()) {
            val clusterConfig = redissonConfig.useClusterServers()
                .setClientName(serviceId.toName()).also {
                    if(config.password.isNotEmpty()) it.setPassword(config.password)
                }
            if (!config.username.isNullOrEmpty()) {
                clusterConfig.setUsername(config.username)
            }
            config.nodes.forEach { node ->
                clusterConfig.addNodeAddress(node.toConnectionString())
            }
            clusterConfig.setSlaveConnectionPoolSize(connectionPoolSize)
                .setMasterConnectionPoolSize(connectionPoolSize)
                .setMasterConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
        }else {
            val singleConfig = redissonConfig.useSingleServer()
                .setAddress(config.nodes.first().toConnectionString())
                .setDatabase(config.databaseId)
                .setClientName(serviceId.toName())
                .setConnectionPoolSize(connectionPoolSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize).also {
                    if(config.password.isNotEmpty()) it.setPassword(config.password)
                }
            if (!config.username.isNullOrEmpty()) {
                singleConfig.setUsername(config.username)
            }
        }
    }

    override suspend fun connect() {
        client = Redisson.create(redissonConfig)
        LOGGER.fine("Successfully connected to redis")
    }

    override suspend fun disconnect() {
        if (connected) client!!.shutdown()
        LOGGER.fine("Successfully disconnected from redis")
    }

    override val connected: Boolean
        get() {
            return client.takeIf { it != null }?.let {
                !it.isShuttingDown
            } ?: false
        }

    fun getClient(): RedissonClient = client!!

}

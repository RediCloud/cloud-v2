package dev.redicloud.database

import dev.redicloud.api.database.IDatabaseConnection
import dev.redicloud.api.database.communication.ICommunicationChannel
import dev.redicloud.api.database.grid.bucket.IDataBucket
import dev.redicloud.api.database.grid.list.ISyncedList
import dev.redicloud.api.database.grid.list.ISyncedMutableList
import dev.redicloud.api.database.grid.lock.ISyncedLock
import dev.redicloud.api.database.grid.map.ISyncedMap
import dev.redicloud.api.database.grid.map.ISyncedMutableMap
import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMap
import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMutableMap
import dev.redicloud.api.exceptions.CloudDatabaseException
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.communication.CommunicationChannel
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.database.grid.bucket.DataBucket
import dev.redicloud.database.grid.list.SyncedList
import dev.redicloud.database.grid.list.SyncedMutableList
import dev.redicloud.database.grid.lock.SyncedLock
import dev.redicloud.database.grid.map.SyncedMap
import dev.redicloud.database.grid.map.SyncedMutableMap
import dev.redicloud.database.grid.map.cache.SyncedCacheMap
import dev.redicloud.database.grid.map.cache.SyncedCacheMutableMap
import dev.redicloud.logging.LogManager
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config

/**
 * Redisson-based implementation of [IDatabaseConnection].
 *
 * Manages connection to a Redis instance (single or cluster mode) and provides
 * factory methods for distributed data structures such as maps, lists, locks, and buckets.
 * Connection pool sizes are automatically halved for Minecraft server service types.
 *
 * @param config the database configuration containing nodes, credentials, and mode
 * @param serviceId the identity of the service owning this connection
 * @param connectionPoolSize maximum number of connections in the pool
 * @param connectionMinimumIdleSize minimum number of idle connections maintained
 * @param subscriptionConnectionPoolSize maximum number of subscription connections
 * @param subscriptionConnectionMinimumIdleSize minimum number of idle subscription connections
 */
class DatabaseConnection(
    config: DatabaseConfiguration,
    override val serviceId: ServiceId,
    connectionPoolSize: Int =
        if (serviceId.type == ServiceType.MINECRAFT_SERVER) {
            DEFAULT_CONNECTION_POOL_SIZE / 2
        } else {
            DEFAULT_CONNECTION_POOL_SIZE
        },
    connectionMinimumIdleSize: Int =
        if (serviceId.type == ServiceType.MINECRAFT_SERVER) {
            DEFAULT_MIN_IDLE_CONNECTIONS / 2
        } else {
            DEFAULT_MIN_IDLE_CONNECTIONS
        },
    subscriptionConnectionPoolSize: Int =
        if (serviceId.type == ServiceType.MINECRAFT_SERVER) {
            DEFAULT_SUBSCRIPTION_POOL_SIZE / 2
        } else {
            DEFAULT_SUBSCRIPTION_POOL_SIZE
        },
    subscriptionConnectionMinimumIdleSize: Int =
        if (serviceId.type == ServiceType.MINECRAFT_SERVER) {
            2 / 2
        } else {
            2
        }
) : IDatabaseConnection {

    companion object {
        private val LOGGER = LogManager.logger(DatabaseConnection::class)
        private const val DEFAULT_CONNECTION_POOL_SIZE = 64
        private const val DEFAULT_MIN_IDLE_CONNECTIONS = 24
        private const val DEFAULT_SUBSCRIPTION_POOL_SIZE = 50
    }

    private val redissonConfig = Config()
    private var _client: RedissonClient? = null

    init {
        redissonConfig
            .setCodec(GsonCodec)
        if (config.isCluster()) {
            val clusterConfig = redissonConfig.useClusterServers()
                .setClientName(serviceId.toName())
                .also {
                    if (!config.password.isNullOrEmpty()) {
                        it.setPassword(config.password)
                    }
                    if (!config.username.isNullOrEmpty()) {
                        it.setUsername(config.username)
                    }
                }
            config.nodes.forEach { node ->
                clusterConfig.addNodeAddress(node.toConnectionString())
            }
            clusterConfig.setSlaveConnectionPoolSize(connectionPoolSize)
                .setMasterConnectionPoolSize(connectionPoolSize)
                .setMasterConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
        } else {
            val singleConfig = redissonConfig.useSingleServer()
                .setAddress(config.nodes.first().toConnectionString())
                .setDatabase(config.databaseId)
                .setClientName(serviceId.toName())
                .setConnectionPoolSize(connectionPoolSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
                .also {
                    if (!config.password.isNullOrEmpty()) {
                        it.setPassword(config.password)
                    }
                }
            if (!config.username.isNullOrEmpty()) {
                singleConfig.setUsername(config.username)
            }
        }
    }

    override suspend fun connect() {
        @Suppress("TooGenericExceptionCaught")
        try {
            _client = Redisson.create(redissonConfig)
            LOGGER.fine("Successfully connected to redis")
        } catch (e: CloudDatabaseException) {
            throw e
        } catch (e: Exception) {
            throw CloudDatabaseException("Failed to connect to database", e)
        }
    }

    override suspend fun disconnect() {
        if (connected) _client!!.shutdown()
        LOGGER.fine("Successfully disconnected from redis")
    }

    override val connected: Boolean
        get() {
            return _client.takeIf { it != null }?.let {
                !it.isShuttingDown
            } ?: false
        }

    override fun <E> getMutableList(key: String): ISyncedMutableList<E> {
        return SyncedMutableList(key, this)
    }

    override fun <E> getList(key: String): ISyncedList<E> {
        return SyncedList(key, this)
    }

    override fun <K, V> getMap(key: String): ISyncedMap<K, V> {
        return SyncedMap(key, this)
    }

    override fun <K, V> getMutableMap(key: String): ISyncedMutableMap<K, V> {
        return SyncedMutableMap(key, this)
    }

    override fun <K, V> getCacheMap(key: String): ISyncedCacheMap<K, V> {
        return SyncedCacheMap(key, this)
    }

    override fun <K, V> getCacheMutableMap(key: String): ISyncedCacheMutableMap<K, V> {
        return SyncedCacheMutableMap(key, this)
    }

    override fun getCommunicationChannel(key: String): ICommunicationChannel {
        return CommunicationChannel(key, this)
    }

    override fun getLock(key: String): ISyncedLock {
        return SyncedLock(key, this)
    }

    override fun getKeysByPattern(pattern: String): List<String> {
        return client.keys.getKeysByPattern(pattern).toList()
    }

    override fun <V> getBucket(key: String): IDataBucket<V> {
        return DataBucket(key, this)
    }

    /** The underlying Redisson client. Throws if not connected. */
    val client: RedissonClient
        get() = _client ?: error("Not connected to redis!")
}

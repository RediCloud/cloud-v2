package dev.redicloud.database

import dev.redicloud.api.database.*
import dev.redicloud.api.database.communication.ICommunicationChannel
import dev.redicloud.api.database.grid.bucket.IDataBucket
import dev.redicloud.api.database.grid.list.ISyncedList
import dev.redicloud.api.database.grid.list.ISyncedMutableList
import dev.redicloud.api.database.grid.lock.ISyncedLock
import dev.redicloud.api.database.grid.map.ISyncedMap
import dev.redicloud.api.database.grid.map.ISyncedMutableMap
import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMap
import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMutableMap
import dev.redicloud.database.codec.GsonCodec
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.logging.LogManager
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.database.communication.CommunicationChannel
import dev.redicloud.database.grid.bucket.DataBucket
import dev.redicloud.database.grid.list.SyncedList
import dev.redicloud.database.grid.map.SyncedMap
import dev.redicloud.database.grid.list.SyncedMutableList
import dev.redicloud.database.grid.lock.SyncedLock
import dev.redicloud.database.grid.map.SyncedMutableMap
import dev.redicloud.database.grid.map.cache.SyncedCacheMap
import dev.redicloud.database.grid.map.cache.SyncedCacheMutableMap
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.BaseCodec
import org.redisson.config.Config

class DatabaseConnection(
    config: DatabaseConfiguration,
    override val serviceId: ServiceId,
    connectionPoolSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 64/2 else 64,
    connectionMinimumIdleSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 24/2 else 24,
    subscriptionConnectionPoolSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 50/2 else 50,
    subscriptionConnectionMinimumIdleSize: Int = if (serviceId.type == ServiceType.MINECRAFT_SERVER) 2/2 else 2
) : IDatabaseConnection {

    companion object {
        private val LOGGER = LogManager.logger(DatabaseConnection::class)
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
                    if(!config.password.isNullOrEmpty()) {
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
        }else {
            val singleConfig = redissonConfig.useSingleServer()
                .setAddress(config.nodes.first().toConnectionString())
                .setDatabase(config.databaseId)
                .setClientName(serviceId.toName())
                .setConnectionPoolSize(connectionPoolSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
                .also {
                    if(!config.password.isNullOrEmpty()) {
                        it.setPassword(config.password)
                    }
                }
            if (!config.username.isNullOrEmpty()) {
                singleConfig.setUsername(config.username)
            }
        }
    }

    override suspend fun connect() {
        _client = Redisson.create(redissonConfig)
        LOGGER.fine("Successfully connected to redis")
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

    val client: RedissonClient
        get() = _client ?: throw IllegalStateException("Not connected to redis!")

}

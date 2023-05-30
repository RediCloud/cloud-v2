package dev.redicloud.database

import dev.redicloud.database.config.DatabaseConfig
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.database.repository.DatabaseRepository
import dev.redicloud.utils.ServiceId
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.BaseCodec
import org.redisson.config.Config

class DatabaseConnection(private val config: DatabaseConfig, serviceId: ServiceId, val codec: BaseCodec) {

    private val redissonConfig = Config()
    var client: RedissonClient? = null
    private val repositories = mutableListOf<DatabaseRepository<*>>()

    init {
        redissonConfig.setCodec(codec)
        if (config.isCluster()) {
            val clusterConfig = redissonConfig.useClusterServers()
                .setClientName(serviceId.toName())
                .setPassword(config.password)
            config.nodes.forEach { node ->
                clusterConfig.addNodeAddress(node.toConnectionString())
            }
        }else {
            redissonConfig.useSingleServer()
                .setAddress(config.nodes.first().toConnectionString())
                .setDatabase(config.databaseId)
                .setClientName(serviceId.toName())
                .setPassword(config.password)
        }
    }

    fun connect() {
        client = Redisson.create(redissonConfig)
    }

    fun disconnect() {
        if (isConnected()) client!!.shutdown()
    }

    fun isConnected(): Boolean {
        if (client == null) return false
        return client!!.isShuttingDown
    }

    fun <T> createBucketRepository(name: String): DatabaseBucketRepository<T> {
        val repository = DatabaseBucketRepository<T>(this, name)
        repositories.add(repository)
        return repository
    }

    fun <T> getBucketRepository(name: String): DatabaseBucketRepository<T>? {
        return repositories.firstOrNull { it.name == name } as DatabaseBucketRepository<T>?
    }

}
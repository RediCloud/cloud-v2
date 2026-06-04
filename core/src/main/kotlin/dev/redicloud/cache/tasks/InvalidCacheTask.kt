package dev.redicloud.cache.tasks

import dev.redicloud.cache.ClusterCache
import dev.redicloud.tasks.CloudTask

/**
 * Periodic task that evicts expired entries from all registered cluster caches.
 *
 * Iterates over every [ClusterCache] and removes entries whose cache duration has elapsed.
 */
class InvalidCacheTask : CloudTask() {

    override suspend fun execute(): Boolean {
        ClusterCache.CACHES.values.forEach {
            it.getCache().forEach { (key, value) ->
                if (it.isCacheValid(key)) return@forEach
                it.setCached(key, null)
            }
        }

        return false
    }
}

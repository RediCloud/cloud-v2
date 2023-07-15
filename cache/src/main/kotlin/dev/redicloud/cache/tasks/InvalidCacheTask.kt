package dev.redicloud.cache.tasks

import dev.redicloud.cache.ClusterCache
import dev.redicloud.tasks.CloudTask

class InvalidCacheTask : CloudTask() {

    override suspend fun execute(): Boolean {

        ClusterCache.CACHES.values.forEach {
            it.getCache().forEach { key, value ->
                if (it.isCacheValid(key)) return@forEach
                it.setCached(key, null)
            }
        }

        return false
    }

}
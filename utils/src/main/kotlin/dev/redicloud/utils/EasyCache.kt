package dev.redicloud.utils

import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

open class EasyCache<T, I>(
    val cacheTime: Duration,
    val block: suspend (I?) -> T?
) {

    private val cachedValues: ConcurrentHashMap<I, Pair<Long, T?>> = ConcurrentHashMap()

    @Volatile
    private var singleCachedValue: Pair<Long, T?>? = null

    suspend fun get(key: I? = null): T? {
        if (!isCacheValid(key)) {
            setCached(key, block(key))
        }
        return getCached(key)
    }

    /**
     * Blocking bridge for non-suspend callers. Prefer [get] in coroutine contexts.
     * Should be removed once all callers support suspend.
     */
    fun getBlocking(key: I? = null): T? = runBlocking { get(key) }

    fun isCached(key: I?): Boolean {
        if (key == null) return singleCachedValue != null
        return cachedValues.containsKey(key)
    }

    fun getCached(key: I?): T? {
        if (key == null) return singleCachedValue?.second
        return cachedValues[key]?.second
    }

    fun setCached(key: I?, value: T?) {
        if (key == null) {
            singleCachedValue = System.currentTimeMillis() to value
            return
        }
        cachedValues[key] = System.currentTimeMillis() to value
    }

    fun isCacheValid(key: I?): Boolean {
        return isCached(key) && System.currentTimeMillis() - (
            if (key != null) {
                cachedValues[key]!!.first
            } else {
                singleCachedValue?.first
                    ?: -1
            }
            ) < cacheTime.inWholeMilliseconds
    }
}

class SingleCache<I>(
    cacheTime: Duration,
    block: suspend (Unit?) -> I?
) : EasyCache<I, Unit>(cacheTime, block)

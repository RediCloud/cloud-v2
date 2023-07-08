package dev.redicloud.utils

import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

open class EasyCache<T, I>(
    val cacheTime: Duration,
    val block: suspend (I?) -> T?
) {

    private var cachedValues: MutableMap<I, Pair<Long, T?>> = mutableMapOf()
    private var singleCachedValue: Pair<Long, T?>? = null

    fun get(key: I? = null): T? {
        if (!isCacheValid(key)) {
            setCached(key, runBlocking { block(key) })
        }
        return getCached(key)
    }

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
        return isCached(key) && System.currentTimeMillis() - (if (key != null) cachedValues[key]!!.first else singleCachedValue?.first
            ?: -1) < cacheTime.inWholeMilliseconds
    }

}

class SingleCache<I>(
    cacheTime: Duration,
    block: suspend (Unit?) -> I?
) : EasyCache<I, Unit>(cacheTime, block)
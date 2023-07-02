package dev.redicloud.utils

import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

open class EasyCache<T, I>(
    val cacheTime: Duration,
    val block: suspend (I?) -> T?
) {

    private var cachedValues: MutableMap<I, Pair<Long, T?>> = mutableMapOf()
    private var singleCachedValue: Pair<Long, T?>? = null

    fun get(value: I? = null): T? {
        if (!isCacheValid(value)) {
            setCached(value, runBlocking { block(value) })
        }
        return getCached(value)
    }

    fun isCached(value: I?): Boolean {
        if (value == null) return singleCachedValue != null
        return cachedValues.containsKey(value)
    }

    fun getCached(value: I?): T? {
        if (value == null) return singleCachedValue?.second
        return cachedValues[value]?.second
    }

    fun setCached(value: I?, newValue: T?) {
        if (value == null) {
            singleCachedValue = System.currentTimeMillis() to newValue
            return
        }
        cachedValues[value] = System.currentTimeMillis() to newValue
    }

    fun isCacheValid(value: I?): Boolean {
        return isCached(value) && System.currentTimeMillis() - (if (value != null) cachedValues[value]!!.first else singleCachedValue?.first
            ?: -1) < cacheTime.inWholeMilliseconds
    }

}

class SingleCache<I>(
    cacheTime: Duration,
    block: suspend (Unit?) -> I?
) : EasyCache<I, Unit>(cacheTime, block)
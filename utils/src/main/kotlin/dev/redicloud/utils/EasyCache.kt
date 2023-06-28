package dev.redicloud.utils

import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class EasyCache<T, I>(
    val cacheTime: Duration,
    val block: suspend (I?) -> T?
) {

    private var cachedValue: T? = null
    private var lastUpdate: Long = -1L

    fun get(value: I? = null): T? {
        if (cachedValue == null || System.currentTimeMillis() - lastUpdate > cacheTime.inWholeMilliseconds) {
            cachedValue = runBlocking { block(value) }
            lastUpdate = System.currentTimeMillis()
        }
        return cachedValue
    }

}
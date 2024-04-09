package dev.redicloud.api.database

import dev.redicloud.api.database.communication.ICommunicationChannel
import dev.redicloud.api.database.grid.list.ISyncedList
import dev.redicloud.api.database.grid.list.ISyncedMutableList
import dev.redicloud.api.database.grid.lock.ISyncedLock
import dev.redicloud.api.database.grid.map.ISyncedMap
import dev.redicloud.api.database.grid.map.ISyncedMutableMap
import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMap
import dev.redicloud.api.database.grid.map.cache.ISyncedCacheMutableMap

interface IDatabaseConnection {

    suspend fun connect()
    suspend fun disconnect()
    val connected: Boolean

    fun <E> getMutableList(key: String): ISyncedMutableList<E>
    fun <E> getList(key: String): ISyncedList<E>

    fun <K, V> getMap(key: String): ISyncedMap<K, V>
    fun <K, V> getMutableMap(key: String): ISyncedMutableMap<K, V>

    fun <K, V> getCacheMap(key: String): ISyncedCacheMap<K, V>
    fun <K, V> getCacheMutableMap(key: String): ISyncedCacheMutableMap<K, V>

    fun getLock(key: String): ISyncedLock

    fun getCommunicationChannel(key: String): ICommunicationChannel

}
package dev.redicloud.service.base.utils

import com.google.gson.reflect.TypeToken
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.utils.gson.gson
import org.redisson.api.options.LocalCachedMapOptions
import java.util.*
import kotlin.collections.ArrayList

class ClusterConfiguration(
    databaseConnection: DatabaseConnection
) {

    val map = databaseConnection.getClient()
        .getLocalCachedMap(LocalCachedMapOptions.name<String, String>("cloud:cluster-configuration")
                .storeMode(LocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS)
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.UPDATE)
        )

    init {
        if (!contains("id")) set("id", UUID.randomUUID().toString())
        if (!contains("proxy-secret")) {
            set("proxy-secret",
                (1..27).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".random() }
                    .joinToString("")
            )
        }
    }

    fun get(key: String): String? {
        return map[key]
    }

    inline fun <reified T> getList(key: String): List<T> {
        val type = object : TypeToken<java.util.ArrayList<T>>() {}.type
        return gson.fromJson(map[key], type) ?: emptyList()
    }

    inline fun <reified T> get(key: String): T? {
        return gson.fromJson(map[key], T::class.java)
    }

    fun set(key: String, value: String) {
        map[key] = value
    }

    inline fun <reified T> set(key: String, value: T) {
        map[key] = gson.toJson(value)
    }

    fun remove(key: String) {
        map.remove(key)
    }

    fun contains(key: String): Boolean {
        return map.containsKey(key)
    }

    fun clear() {
        map.clear()
    }

}
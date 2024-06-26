package dev.redicloud.service.base.utils

import dev.redicloud.database.DatabaseConnection
import dev.redicloud.utils.gson.fromJsonToList
import dev.redicloud.utils.gson.gson
import java.util.*

class ClusterConfiguration(
    databaseConnection: DatabaseConnection
) {

    val map = databaseConnection.getCacheMutableMap<String, String>("cloud:cluster-configuration")

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

    inline fun <reified T> getList(key: String, defaultValue: List<T>? = null): List<T> {
        if (!contains(key)) {
            return defaultValue ?: throw Exception("Key $key not found")
        }
        return gson.fromJsonToList(map[key]!!)
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
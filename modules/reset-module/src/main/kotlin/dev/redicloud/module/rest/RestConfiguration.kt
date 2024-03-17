package dev.redicloud.module.rest

import com.google.gson.reflect.TypeToken
import dev.redicloud.utils.gson.gson
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions
import java.util.ArrayList

class RestConfiguration(
    redissonClient: RedissonClient
) {

    val map = redissonClient.getLocalCachedMap(LocalCachedMapOptions.name<String, String>("module:rest-server:config"))

    fun get(key: String): String? {
        return map[key]
    }

    inline fun <reified T> getList(key: String): List<T> {
        val type = object : TypeToken<ArrayList<T>>() {}.type
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
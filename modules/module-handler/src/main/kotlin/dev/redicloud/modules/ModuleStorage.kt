package dev.redicloud.modules

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.utils.gson.gson

class ModuleStorage(
    override val moduleId: String,
    override val name: String,
    databaseConnection: DatabaseConnection
) : IModuleStorage {

    private val map = databaseConnection.getCacheMutableMap<String, String>("module-storage:$moduleId:$name")

    override suspend fun remove(key: String) {
        map.remove(key)
    }

    override suspend fun clear() {
        map.clear()
    }

    override suspend fun keys(): Set<String> {
        return map.keys
    }

    override suspend fun values(): Collection<String> {
        return map.values
    }

    override suspend fun size(): Int {
        return map.size
    }

    override suspend fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override suspend fun isNotEmpty(): Boolean {
        return map.isNotEmpty()
    }

    override suspend fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    override suspend fun <T> getOrDefault(key: String, defaultValue: () -> T, clazz: Class<T>): T {
        val value = map[key] ?: return run {
            val value = defaultValue()
            set(key, value)
            value
        }
        return gson.fromJson(value, clazz)
    }

    override suspend fun <T> get(key: String, clazz: Class<T>): T? {
        val value = map[key] ?: return null
        return gson.fromJson(value, clazz)
    }

    override suspend fun <T> set(key: String, value: T) {
        map[key] = gson.toJson(value)
    }

    override suspend fun <T> getList(key: String, clazz: Class<T>): List<T> {
        val value = map[key] ?: return emptyList()
        return gson.fromJson(value, List::class.java).map { gson.fromJson(it.toString(), clazz) }
    }

    override suspend fun <T> getListOrDefault(key: String, clazz: Class<T>, defaultValue: () -> List<T>): List<T> {
        val value = map[key] ?: return run {
            val value = defaultValue()
            set(key, value)
            value
        }
        return gson.fromJson(value, List::class.java).map { gson.fromJson(it.toString(), clazz) }
    }

}
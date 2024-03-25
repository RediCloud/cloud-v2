package dev.redicloud.modules

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.utils.gson.gson
import org.redisson.api.options.LocalCachedMapOptions

class ModuleStorage(
    override val moduleId: String,
    override val name: String,
    databaseConnection: DatabaseConnection
) : IModuleStorage {

    private val map = databaseConnection.getClient().getLocalCachedMap<String, String>(
        LocalCachedMapOptions.name("module-storage:$moduleId:$name")
    )

    override fun remove(key: String) {
        map.remove(key)
    }

    override fun clear() {
        map.clear()
    }

    override fun keys(): Set<String> {
        return map.keys
    }

    override fun values(): Collection<String> {
        return map.values
    }

    override fun size(): Int {
        return map.size
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun isNotEmpty(): Boolean {
        return map.isNotEmpty()
    }

    override fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    override fun <T> getOrDefault(key: String, defaultValue: () -> T, clazz: Class<T>): T {
        val value = map[key] ?: return run {
            val value = defaultValue()
            set(key, value)
            value
        }
        return gson.fromJson(value, clazz)
    }

    override fun <T> get(key: String, clazz: Class<T>): T? {
        val value = map[key] ?: return null
        return gson.fromJson(value, clazz)
    }

    override fun <T> set(key: String, value: T) {
        map[key] = gson.toJson(value)
    }

}
package dev.redicloud.api.modules

interface IModuleStorage {

    val moduleId: String
    val name: String

    fun remove(key: String)

    fun clear()

    fun keys(): Set<String>

    fun values(): Collection<String>

    fun size(): Int

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean

    fun containsKey(key: String): Boolean

    fun <T> get(key: String, clazz: Class<T>): T?

    fun <T> getOrDefault(key: String, defaultValue: () -> T, clazz: Class<T>): T

    fun <T> set(key: String, value: T)

}

inline fun <reified T> IModuleStorage.get(key: String): T? = get(key, T::class.java)

inline fun <reified T> IModuleStorage.getOrDefault(key: String, noinline defaultValue: () -> T): T = getOrDefault(key, defaultValue, T::class.java)
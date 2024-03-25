package dev.redicloud.api.modules

interface IModuleStorage {

    val moduleId: String
    val name: String

    suspend fun remove(key: String)

    suspend fun clear()

    suspend fun keys(): Set<String>

    suspend fun values(): Collection<String>

    suspend fun size(): Int

    suspend fun isEmpty(): Boolean

    suspend fun isNotEmpty(): Boolean

    suspend fun containsKey(key: String): Boolean

    suspend fun <T> get(key: String, clazz: Class<T>): T?

    suspend fun <T> getOrDefault(key: String, defaultValue: () -> T, clazz: Class<T>): T

    suspend fun <T> set(key: String, value: T)

}

suspend inline fun <reified T> IModuleStorage.get(key: String): T? = get(key, T::class.java)

suspend inline fun <reified T> IModuleStorage.getOrDefault(key: String, noinline defaultValue: () -> T): T = getOrDefault(key, defaultValue, T::class.java)
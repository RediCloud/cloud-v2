package dev.redicloud.repository.server.version

import com.google.gson.annotations.Expose
import dev.redicloud.utils.prettyPrintGson
import dev.redicloud.utils.versions.JavaVersion
import khttp.get

class MinecraftVersion(
    val name: String,
    val protocolId: Int,
    private val supportedJavaVersion: Array<String>
) {

    @Expose(deserialize = false, serialize = false)
    private val cachedJavaVersions = supportedJavaVersion.mapNotNull { JavaVersion.parse(it) }

    fun isSupported(version: JavaVersion): Boolean = cachedJavaVersions.contains(version)

    companion object {
        private val CACHED_MINECRAFT_VERSIONS = mutableListOf<MinecraftVersion>()

        fun versions(): List<MinecraftVersion> = CACHED_MINECRAFT_VERSIONS

        suspend fun loadOnlineVersions() {
            CACHED_MINECRAFT_VERSIONS.clear()
            val json = get("https://github.com/RediCloud/cloud-v2/api-files/minecraft-versions.json").jsonObject.toString()
            val list = prettyPrintGson.fromJson(json, List::class.java) as List<MinecraftVersion>
            CACHED_MINECRAFT_VERSIONS.addAll(list)
        }
        suspend fun loadIfNotLoaded() {
            if (CACHED_MINECRAFT_VERSIONS.isNotEmpty()) return
            loadOnlineVersions()
        }
    }

}


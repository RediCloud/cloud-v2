package dev.redicloud.repository.server.version.utils

import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import dev.redicloud.utils.prettyPrintGson
import dev.redicloud.utils.versions.JavaVersion
import khttp.get

class ServerVersion(
    val name: String,
    val protocolId: Int,
    val versionType: Array<CloudServerVersionType> = CloudServerVersionType.VALUES.toTypedArray(),
    private val supportedJavaVersion: Array<String> = emptyArray()
) {

    @Expose(deserialize = false, serialize = false)
    private val cachedJavaVersions = supportedJavaVersion.mapNotNull { JavaVersion.parse(it) }

    fun isSupported(version: JavaVersion): Boolean = cachedJavaVersions.contains(version) || supportedJavaVersion.isEmpty()

    companion object {
        private val CACHED_MINECRAFT_VERSIONS = mutableListOf<ServerVersion>()

        fun versions(): List<ServerVersion> = CACHED_MINECRAFT_VERSIONS

        suspend fun loadOnlineVersions() {
            CACHED_MINECRAFT_VERSIONS.clear()
            val json = get("https://raw.githubusercontent.com/RediCloud/cloud-v2/master/api-files/server-versions.json").text
            val type = object : TypeToken<ArrayList<ServerVersion>>() {}.type
            val list: List<ServerVersion> = prettyPrintGson.fromJson(json, type)
            CACHED_MINECRAFT_VERSIONS.addAll(list)
        }
        suspend fun loadIfNotLoaded() {
            if (CACHED_MINECRAFT_VERSIONS.isNotEmpty()) return
            loadOnlineVersions()
        }

        fun parse(s: String, strict: Boolean = true): ServerVersion? {
            val t = if (strict) s else s.lowercase().split("-")[0]
            return versions().firstOrNull { it.name.lowercase() == t }
        }
    }

}


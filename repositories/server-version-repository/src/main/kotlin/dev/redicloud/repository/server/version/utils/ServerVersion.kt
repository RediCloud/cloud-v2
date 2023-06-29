package dev.redicloud.repository.server.version.utils

import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import dev.redicloud.utils.prettyPrintGson
import dev.redicloud.utils.versions.JavaVersion
import khttp.get

class ServerVersion(
    val name: String,
    val protocolId: Int,
    val versionType: Array<String> = CloudServerVersionType.VALUES.map { it.name }.toTypedArray(),
    private val supportedJavaVersion: Array<String> = emptyArray()
) {

    fun isUnknown(): Boolean = name == "unknown"

    @Expose(deserialize = false, serialize = false)
    private val cachedJavaVersions = supportedJavaVersion.mapNotNull { JavaVersion.parse(it) }

    fun isSupported(version: JavaVersion): Boolean = (cachedJavaVersions.contains(version) || supportedJavaVersion.isEmpty()) && !isUnknown()

    companion object {
        private val CACHED_MINECRAFT_VERSIONS = mutableListOf<ServerVersion>()

        fun versions(): List<ServerVersion> = CACHED_MINECRAFT_VERSIONS

        suspend fun loadOnlineVersions() {
            CACHED_MINECRAFT_VERSIONS.clear()
            val json = get("https://raw.githubusercontent.com/RediCloud/cloud-v2/master/api-files/server-versions.json").text
            val type = object : TypeToken<ArrayList<ServerVersion>>() {}.type
            val list = prettyPrintGson.fromJson<List<ServerVersion>?>(json, type).toMutableList()
            if (list.none { it.isUnknown() }) {
                list.add(ServerVersion("unknown", -1))
            }
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


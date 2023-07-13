package dev.redicloud.repository.server.version.utils

import com.google.gson.reflect.TypeToken
import dev.redicloud.utils.getAPIUrl
import dev.redicloud.utils.gson.gson
import khttp.get

class ServerVersion(
    val name: String,
    val protocolId: Int,
    val versionTypes: Array<String> = arrayOf()
) {

    fun isUnknown(): Boolean = name.lowercase() == "unknown"

    companion object {
        private val CACHED_MINECRAFT_VERSIONS = mutableListOf<ServerVersion>()

        fun versions(): List<ServerVersion> = CACHED_MINECRAFT_VERSIONS

        suspend fun loadOnlineVersions() {
            CACHED_MINECRAFT_VERSIONS.clear()
            val json = get("${getAPIUrl()}/api-files/server-versions.json").text
            val type = object : TypeToken<ArrayList<ServerVersion>>() {}.type
            val list = gson.fromJson<List<ServerVersion>?>(json, type).toMutableList()
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


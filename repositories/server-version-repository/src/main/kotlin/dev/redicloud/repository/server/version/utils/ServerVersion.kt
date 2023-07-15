package dev.redicloud.repository.server.version.utils

import com.google.gson.reflect.TypeToken
import dev.redicloud.utils.getAPIUrlOrFallback
import dev.redicloud.utils.getTextOfAPIWithFallback
import dev.redicloud.utils.gson.gson
import khttp.get

class ServerVersion(
    val name: String,
    val protocolId: Int,
    val versionTypes: Array<String> = arrayOf()
) {

    fun isUnknown(): Boolean = name.lowercase() == "unknown"

    fun isLatest(): Boolean = name.lowercase() == "latest"

    fun isLatestMcVersion(): Boolean {
        return versions().first { it.isLatest() }.dynamicVersion()?.name == this.name
    }

    fun isMcVersion(): Boolean = mcVersionRegex.matches(name)

    fun dynamicVersion(): ServerVersion? {
        return if (name.lowercase() == "latest") {
            val versionComparator = compareBy<ServerVersion> { it.protocolId }
                .thenByDescending { it.name.substringBefore('.') }
                .thenByDescending { it.name.substringAfter('.').substringBefore('.') }
                .thenByDescending { it.name.substringAfterLast('.') }

            val versions = versions().toMutableList()
            versions.removeIf { !it.isMcVersion() }
            val sortedVersions = versions.sortedWith(versionComparator)

            return sortedVersions.lastOrNull()
        }else {
            null
        }
    }

    companion object {
        private val mcVersionRegex = Regex("(\\d+(\\.\\d+)+)")
        val CACHED_MINECRAFT_VERSIONS = mutableListOf<ServerVersion>() //TODO private add

        fun versions(): MutableList<ServerVersion> = CACHED_MINECRAFT_VERSIONS

        suspend fun loadOnlineVersions() {
            CACHED_MINECRAFT_VERSIONS.clear()
            val json = getTextOfAPIWithFallback("api-files/server-versions.json")
            val type = object : TypeToken<ArrayList<ServerVersion>>() {}.type
            val list = gson.fromJson<List<ServerVersion>?>(json, type).toMutableList()
            if (list.none { it.isUnknown() }) {
                list.add(ServerVersion("unknown", -1, emptyArray()))
            }
            if (list.none { it.isLatest() }) {
                list.add(ServerVersion("latest", -1, emptyArray()))
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


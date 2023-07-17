package dev.redicloud.repository.server.version.utils

import com.google.gson.reflect.TypeToken
import dev.redicloud.api.repositories.version.IServerVersion
import dev.redicloud.utils.getTextOfAPIWithFallback
import dev.redicloud.utils.gson.gson

class ServerVersion(
    override val name: String,
    override val protocolId: Int,
    override val versionTypes: Array<String> = arrayOf()
) : Comparable<IServerVersion>, IServerVersion {

    override val unknown: Boolean
        get() {
            return name.lowercase() == "unknown"
        }

    override val latest: Boolean
        get() {
            return name.lowercase() == "latest"
        }

    override val latestMcVersion: Boolean
        get() {
            return versions().first { it.latest }.dynamicVersion().name == this.name
        }

    override val mcVersion: Boolean
        get() {
            return mcVersionRegex.matches(name)
        }

    override fun dynamicVersion(): ServerVersion {
        return if (name.lowercase() == "latest") {
            val versions = versions().toMutableList()
            versions.removeIf { !it.mcVersion }
            return versions.sortedWith(VERSION_COMPARATOR).lastOrNull() ?: this
        }else {
            this
        }
    }

    companion object {
        private val mcVersionRegex = Regex("(\\d+(\\.\\d+)+)")
        val CACHED_MINECRAFT_VERSIONS = mutableListOf<ServerVersion>() //TODO private add

        val VERSION_COMPARATOR = compareBy<IServerVersion> { if (it.latest) 1000 else it.protocolId }
            .thenByDescending { it.name.substringBefore('.') }
            .thenByDescending { it.name.substringAfter('.').substringBefore('.') }
            .thenByDescending { it.name.substringAfterLast('.') }
        fun versions(): MutableList<ServerVersion> = CACHED_MINECRAFT_VERSIONS

        suspend fun loadOnlineVersions() {
            CACHED_MINECRAFT_VERSIONS.clear()
            val json = getTextOfAPIWithFallback("api-files/server-versions.json")
            val type = object : TypeToken<ArrayList<ServerVersion>>() {}.type
            val list = gson.fromJson<List<ServerVersion>?>(json, type).toMutableList()
            if (list.none { it.unknown }) {
                list.add(ServerVersion("unknown", -1, emptyArray()))
            }
            if (list.none { it.latest }) {
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

    override fun compareTo(other: IServerVersion): Int {
        return VERSION_COMPARATOR.compare(this, other)
    }

}


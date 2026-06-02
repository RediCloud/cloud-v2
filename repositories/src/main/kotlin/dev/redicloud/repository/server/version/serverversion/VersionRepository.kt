package dev.redicloud.repository.server.version.serverversion

import dev.redicloud.api.version.IServerVersion
import dev.redicloud.api.version.IVersionRepository
import dev.redicloud.utils.getTextFromGitHub
import dev.redicloud.utils.gson.fromJsonToList
import dev.redicloud.utils.gson.gson

object VersionRepository : IVersionRepository {

    val mcVersionRegex = Regex("(\\d+(\\.\\d+)+)")

    val cachedVersions = mutableListOf<ServerVersion>() // TODO private add

    val versionComparator = compareBy<IServerVersion> { if (it.latest) 1000 else it.protocolId }
    override fun versions(): MutableList<ServerVersion> = cachedVersions

    override suspend fun loadOnlineVersions() {
        cachedVersions.clear()
        val json = getTextFromGitHub("api-files/server-versions.json")
        val list = gson.fromJsonToList<ServerVersion>(json).toMutableList()
        if (list.none { it.unknown }) {
            list.add(ServerVersion("unknown", -1, emptyArray()))
        }
        if (list.none { it.latest }) {
            list.add(ServerVersion("latest", -1, emptyArray()))
        }
        cachedVersions.addAll(list)
    }

    override suspend fun loadIfNotLoaded() {
        if (cachedVersions.isNotEmpty()) return
        loadOnlineVersions()
    }

    override fun parse(s: String, strict: Boolean): ServerVersion? {
        val t = if (strict) s else s.split("-")[0]
        return if (strict) {
            versions().firstOrNull {
                it.name == t
            }
        } else {
            versions().firstOrNull { it.name.lowercase() == t.lowercase() }
        }
    }
}

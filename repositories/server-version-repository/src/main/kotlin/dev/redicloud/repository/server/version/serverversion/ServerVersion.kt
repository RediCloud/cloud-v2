package dev.redicloud.repository.server.version.serverversion

import dev.redicloud.api.version.IServerVersion
import dev.redicloud.repository.server.version.serverversion.VersionRepository.versionComparator

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
            return VersionRepository.versions().first { it.latest }.dynamicVersion().name == this.name
        }

    override val mcVersion: Boolean
        get() {
            return VersionRepository.mcVersionRegex.matches(name)
        }

    override fun dynamicVersion(): ServerVersion {
        return if (name.lowercase() == "latest") {
            val versions = VersionRepository.versions().toMutableList()
            versions.removeIf { !it.mcVersion }
            return versions.sortedWith(versionComparator).lastOrNull() ?: this
        }else {
            this
        }
    }

    override fun compareTo(other: IServerVersion): Int {
        return versionComparator.compare(this, other)
    }

}


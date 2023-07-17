package dev.redicloud.api.repositories.version

interface IServerVersion : Comparable<IServerVersion> {

    val name: String
    val protocolId: Int
    val versionTypes: Array<String>
    val unknown: Boolean
    val latest: Boolean
    val latestMcVersion: Boolean
    val mcVersion: Boolean

    fun dynamicVersion(): IServerVersion

}
package dev.redicloud.api.version

interface IVersionRepository {

    fun versions(): List<IServerVersion>

    suspend fun loadOnlineVersions()

    suspend fun loadIfNotLoaded()

    fun parse(s: String, strict: Boolean = true): IServerVersion?

}
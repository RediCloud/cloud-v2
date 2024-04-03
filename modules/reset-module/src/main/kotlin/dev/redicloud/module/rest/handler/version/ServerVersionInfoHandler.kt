package dev.redicloud.module.rest.handler.version

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.version.ICloudServerVersionRepository
import dev.redicloud.module.rest.RestHandler
import dev.redicloud.module.rest.fetcher.ServerVersionFetcher
import io.javalin.http.Context

class ServerVersionInfoHandler(
    private val serverVersionRepository: ICloudServerVersionRepository,
    private val serverVersionFetcher: ServerVersionFetcher,
    config: IModuleStorage
) : RestHandler(config, "/server-version/info/"){

    override suspend fun handleRequest(ctx: Context) {
        if (ctx.queryParam("id") != null) {
            val serverVersion = serverVersionFetcher.fetchVersionById(ctx.pathParam("id"))
            if (serverVersion == null) {
                ctx.status(404)
                ctx.json(mapOf("error" to "Server version not found"))
                return
            }
            ctx.json(serverVersion)
            return
        }
        if (ctx.queryParam("name") != null) {
            val serverVersion = serverVersionFetcher.fetchVersionByName(ctx.pathParam("name"))
            if (serverVersion == null) {
                ctx.status(404)
                ctx.json(mapOf("error" to "Server version not found"))
                return
            }
            ctx.json(serverVersion)
            return
        }
        val versions = serverVersionRepository.getVersions()
        ctx.json(versions)
    }

}
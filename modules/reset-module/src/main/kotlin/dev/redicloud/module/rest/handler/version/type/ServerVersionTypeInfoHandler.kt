package dev.redicloud.module.rest.handler.version.type

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.version.ICloudServerVersionTypeRepository
import dev.redicloud.module.rest.RestHandler
import dev.redicloud.module.rest.fetcher.ServerVersionTypeFetcher
import io.javalin.http.Context

class ServerVersionTypeInfoHandler(
    private val serverVersionTypeRepository: ICloudServerVersionTypeRepository,
    private val serverVersionTypeFetcher: ServerVersionTypeFetcher,
    config: IModuleStorage
) : RestHandler(config, "/server-version/type/") {

    override suspend fun handleRequest(ctx: Context) {
        if (ctx.queryParam("id") != null) {
            val serverVersionType = serverVersionTypeFetcher.fetchVersionTypeById(ctx.pathParam("id"))
            if (serverVersionType == null) {
                ctx.status(404)
                ctx.json(mapOf("error" to "Server version type not found"))
                return
            }
            ctx.json(serverVersionType)
            return
        }
        if (ctx.queryParam("name") != null) {
            val serverVersionType = serverVersionTypeFetcher.fetchVersionTypeByName(ctx.pathParam("name"))
            if (serverVersionType == null) {
                ctx.status(404)
                ctx.json(mapOf("error" to "Server version type not found"))
                return
            }
            ctx.json(serverVersionType)
            return
        }
        val versions = serverVersionTypeRepository.getTypes()
        ctx.json(versions)
    }

}
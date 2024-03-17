package dev.redicloud.module.rest.handler.player

import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.module.rest.RestConfiguration
import dev.redicloud.module.rest.RestHandler
import dev.redicloud.module.rest.parser.PlayerRestParser
import io.javalin.http.Context

class PlayerInfoHandler(
    restConfiguration: RestConfiguration,
    private val playerParser: PlayerRestParser
) : RestHandler(restConfiguration, "/player/info") {

    override suspend fun handleRequest(ctx: Context) {
        val player = if (ctx.queryParam("name") != null) {
            playerParser.parseNameToPlayer { ctx.queryParam("name") }
        }else if (ctx.queryParam("uuid") != null) {
            playerParser.parseUUIDToPlayer { ctx.queryParam("uuid") }
        }else{
            null
        }
        if (player == null) {
            ctx.status(404)
            ctx.json(mapOf("error" to "Player not found"))
            return
        }
        ctx.json(player)
    }

}
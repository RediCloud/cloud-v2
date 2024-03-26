package dev.redicloud.module.rest.handler.player

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.module.rest.RestHandler
import dev.redicloud.module.rest.fetcher.PlayerFetcher
import io.javalin.http.Context

class PlayerInfoHandler(
    private val playerRepository: ICloudPlayerRepository,
    private val playerParser: PlayerFetcher,
    config: IModuleStorage,
) : RestHandler(config, "/player/info") {

    override suspend fun handleRequest(ctx: Context) {
        if (ctx.queryParam("name") != null) {
            val player = playerParser.fetchPlayerByName(ctx.queryParam("name"))
            if (player == null) {
                ctx.status(404)
                ctx.json(mapOf("error" to "Player not found"))
                return
            }
            ctx.json(player)
            return
        }
        if (ctx.queryParam("uuid") != null) {
            val player = playerParser.fetchPlayerByUniqueId(ctx.queryParam("uuid"))
            if (player == null) {
                ctx.status(404)
                ctx.json(mapOf("error" to "Player not found"))
                return
            }
            ctx.json(player)
            return
        }
        val players = if (ctx.queryParam("online")?.toBooleanStrictOrNull() == true) {
            playerRepository.getConnectedPlayers()
        } else {
            playerRepository.getRegisteredPlayers()
        }
        ctx.json(players)
    }

}
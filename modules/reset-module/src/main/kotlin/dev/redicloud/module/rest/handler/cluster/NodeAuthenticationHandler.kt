package dev.redicloud.module.rest.handler.cluster

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.modules.getListOrDefault
import dev.redicloud.api.utils.DATABASE_JSON
import dev.redicloud.module.rest.RestHandler
import dev.redicloud.utils.SingleCache
import io.javalin.http.Context
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class NodeAuthenticationHandler(
    config: IModuleStorage,
    nodeId: UUID
) : RestHandler(config, "cluster/authenticate") {

    private val tokensCache = SingleCache<List<String>>(10.seconds) { config.getListOrDefault("auth-tokens-$nodeId") { emptyList() } }

    override suspend fun handleRequest(ctx: Context) {
        val token = ctx.queryParam("token")
        if (token == null) {
            ctx.status(401)
            ctx.result("Authentication failed")
            return
        }
        if (!tokensCache.get()!!.contains(token)) {
            ctx.status(401)
            ctx.result("Authentication failed")
            return
        }
        ctx.status(200)
        ctx.result(DATABASE_JSON.getFile().readBytes())
    }

}
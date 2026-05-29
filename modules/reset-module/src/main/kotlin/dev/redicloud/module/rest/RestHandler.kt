package dev.redicloud.module.rest

import dev.redicloud.api.modules.IModuleStorage
import dev.redicloud.api.modules.getListOrDefault
import dev.redicloud.utils.SingleCache
import io.javalin.http.Context
import io.javalin.http.Handler
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

const val HTTP_UNAUTHORIZED = 401
const val HTTP_NOT_FOUND = 404
const val HTTP_INTERNAL_ERROR = 500

abstract class RestHandler(
    val config: IModuleStorage,
    val path: String,
    val auth: Boolean = true
) : Handler {

    private val tokensCache = SingleCache<List<String>>(5.seconds) { config.getListOrDefault("tokens") { emptyList() } }

    override fun handle(ctx: Context) {
        runBlocking {
            runCatching {
                if (auth) {
                    val token = ctx.header("redicloud-token")
                    if (token == null) {
                        ctx.json(mapOf("error" to "No token provided"))
                        ctx.status(HTTP_UNAUTHORIZED)
                        return@runBlocking
                    }
                    if (tokensCache.get()!!.none { it == token }) {
                        ctx.json(mapOf("error" to "Invalid token"))
                        ctx.status(HTTP_UNAUTHORIZED)
                        return@runBlocking
                    }
                }
                handleRequest(ctx)
            }.onFailure {
                it.printStackTrace()
                ctx.status(HTTP_INTERNAL_ERROR)
                ctx.json(mapOf("error" to it.message))
            }
        }
    }

    abstract suspend fun handleRequest(ctx: Context)
}

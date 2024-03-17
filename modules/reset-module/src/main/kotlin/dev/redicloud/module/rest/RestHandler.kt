package dev.redicloud.module.rest

import io.javalin.http.Context
import io.javalin.http.Handler
import kotlinx.coroutines.runBlocking

abstract class RestHandler(
    val restConfiguration: RestConfiguration,
    val path: String,
    val auth: Boolean = true
) : Handler {

    override fun handle(ctx: Context) {
        runBlocking {
            runCatching {
                if (auth) {
                    val token = ctx.header("redicloud-token")
                    if (token == null) {
                        ctx.json(mapOf("error" to "No token provided"))
                        ctx.status(401)
                        return@runBlocking
                    }
                    if (restConfiguration.getList<String>("tokens").none { it == token }) {
                        ctx.json(mapOf("error" to "Invalid token"))
                        ctx.status(401)
                        return@runBlocking
                    }
                }
                handleRequest(ctx)
            }.onFailure {
                it.printStackTrace()
                ctx.status(500)
                ctx.json(mapOf("error" to it.message))
            }
        }
    }

    abstract suspend fun handleRequest(ctx: Context)

}
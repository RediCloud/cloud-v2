package dev.redicloud.module.rest

import io.javalin.http.Context
import io.javalin.http.Handler
import kotlinx.coroutines.runBlocking

abstract class RestHandler(
    val path: String
) : Handler {

    override fun handle(ctx: Context) {
        runBlocking {
            runCatching {
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
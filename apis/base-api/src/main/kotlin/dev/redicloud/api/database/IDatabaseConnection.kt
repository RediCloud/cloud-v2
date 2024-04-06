package dev.redicloud.api.database
interface IDatabaseConnection {

    suspend fun connect()
    suspend fun disconnect()
    val connected: Boolean

}
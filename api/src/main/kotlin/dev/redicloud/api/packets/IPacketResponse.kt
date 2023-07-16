package dev.redicloud.api.packets

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface IPacketResponse {

    val manager: IPacketManager
    val packet: AbstractPacket

    fun waitForResponse(responseCount: Int = 1, block: (AbstractPacket?) -> Unit): IPacketResponse

    suspend fun waitBlocking(): AbstractPacket?

    fun withTimeOut(timeOut: Duration): IPacketResponse

    fun withTimeOut(duration: java.time.Duration): IPacketResponse {
        return withTimeOut(duration.toMillis().milliseconds)
    }

    fun withTimeOut(millis: Long): IPacketResponse {
        return withTimeOut(millis.milliseconds)
    }

}
package dev.redicloud.packets

import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PacketResponse(
    override val manager: PacketManager,
    override val packet: AbstractPacket
) : IPacketResponse {

    private val responseQueue = ConcurrentHashMap<(AbstractPacket?) -> Unit, Int>()
    private var timeOut: Duration = 15.seconds
    private val responses = mutableListOf<AbstractPacket?>()
    private var timeOutJob: Job? = null

    override fun waitForResponse(responseCount: Int, block: (AbstractPacket?) -> Unit): IPacketResponse {
        responseQueue[block] = responseCount
        if (!manager.packetResponses.contains(this)) manager.packetResponses.add(this)
        manager.packetsOfLast3Seconds.filter { it.referenceId == packet.packetId }.forEach {
            handle(it)
        }
        timeOutJob = manager.packetScope.launch {
            delay(timeOut.inWholeMilliseconds)
            manager.packetResponses.remove(this@PacketResponse)
        }
        return this
    }

    override suspend fun waitBlocking(): AbstractPacket? {
        val timeOut = System.currentTimeMillis() + this.timeOut.inWholeMilliseconds
        if (!manager.packetResponses.contains(this)) manager.packetResponses.add(this)
        while (responses.isEmpty()) {
            if (System.currentTimeMillis() > timeOut) {
                manager.packetResponses.remove(this)
                timeOutJob?.cancel()
                return null
            }
            delay(this.timeOut.inWholeMilliseconds / 15)
        }
        return responses.first()
    }

    override fun withTimeOut(timeOut: Duration): PacketResponse {
        this.timeOut = timeOut
        return this
    }

    internal fun handle(packet: AbstractPacket) {
        if (packet.referenceId != this.packet.packetId) return
        responses.add(packet)
        val newMap = ConcurrentHashMap(responseQueue)
        newMap.forEach { (block, count) ->
            if (count == 1) {
                responseQueue.remove(block)
            } else {
                responseQueue[block] = count - 1
            }
            block(packet)
        }
        if (responseQueue.isEmpty()) {
            manager.packetResponses.remove(this)
            timeOutJob?.cancel()
        }
    }

}
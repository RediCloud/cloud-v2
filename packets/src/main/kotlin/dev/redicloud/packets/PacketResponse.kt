package dev.redicloud.packets

import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PacketResponse(val packetManager: PacketManager, val packet: AbstractPacket) {

    private val responseQueue = ConcurrentHashMap<(AbstractPacket?) -> Unit, Int>()
    private var timeOut: Duration = 15.seconds
    private val responses = mutableListOf<AbstractPacket?>()
    private var timeOutJob: Job? = null

    fun waitForResponse(responseCount: Int = 1, block: (AbstractPacket?) -> Unit): PacketResponse {
        responseQueue[block] = responseCount
        if (!packetManager.packetResponses.contains(this)) packetManager.packetResponses.add(this)
        packetManager.packetsOfLast3Seconds.filter { it.referenceId == packet.packetId }.forEach {
            handle(it)
        }
        timeOutJob = defaultScope.launch {
            delay(timeOut.inWholeMilliseconds)
            packetManager.packetResponses.remove(this@PacketResponse)
        }
        return this
    }

    fun waitBlocking(): AbstractPacket? {
        val timeOut = System.currentTimeMillis() + this.timeOut.inWholeMilliseconds
        if (!packetManager.packetResponses.contains(this)) packetManager.packetResponses.add(this)
        while (responses.isEmpty()) {
            if (System.currentTimeMillis() > timeOut) {
                packetManager.packetResponses.remove(this)
                timeOutJob?.cancel()
                return null
            }
            Thread.sleep(this.timeOut.inWholeMilliseconds / 15)
        }
        return responses.first()
    }

    fun withTimeOut(timeOut: Duration): PacketResponse {
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
            packetManager.packetResponses.remove(this)
            timeOutJob?.cancel()
        }
    }

}
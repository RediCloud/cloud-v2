package dev.redicloud.tasks.executor

import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.packets.PacketListener
import dev.redicloud.api.packets.listen
import dev.redicloud.tasks.CloudTask
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class PacketBasedCloudExecutor(
    task: CloudTask,
    val packetManager: IPacketManager,
    val packets: List<KClass<out AbstractPacket>>
) : CloudTaskExecutor(task) {

    private val listeners = mutableListOf<PacketListener<*>>()

    override suspend fun run() {
        packets.forEach { listener(it) }
        cloudTask.onFinished {
            listeners.forEach {
                packetManager.unregisterListener(it)
            }
        }
    }

    private fun listener(clazz: KClass<out AbstractPacket>) {
        val listener = packetManager.listen(clazz) {
            cloudTask.taskManager.scope.launch {
                cloudTask.preExecute(this@PacketBasedCloudExecutor)?.join()
            }
        }
        listeners.add(listener)
    }

}
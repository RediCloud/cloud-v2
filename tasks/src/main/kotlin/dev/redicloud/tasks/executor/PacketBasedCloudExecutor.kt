package dev.redicloud.tasks.executor

import dev.redicloud.logging.LogManager
import dev.redicloud.packets.AbstractPacket
import dev.redicloud.packets.PacketListener
import dev.redicloud.packets.PacketManager
import dev.redicloud.tasks.CloudTask
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class PacketBasedCloudExecutor(
    task: CloudTask,
    val packetManager: PacketManager,
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
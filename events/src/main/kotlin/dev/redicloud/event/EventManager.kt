package dev.redicloud.event

import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.gson
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

class EventManager(val identifier: String, val packetManager: PacketManager?) {

    companion object {
        val LOGGER = LogManager.logger(EventManager::class)
        private val MANAGERS = mutableMapOf<String, EventManager>()

        fun getManager(identifier: String): EventManager? = MANAGERS[identifier]
    }

    val handlers: MutableMap<KClass<*>, MutableList<EventHandlerMethod>> = HashMap()
    private val lock = ReentrantLock(true)

    init {
        MANAGERS[identifier] = this
        if (packetManager != null && !packetManager.isPacketRegistered(CloudEventPacket::class)) {
            packetManager.registerPacket(CloudEventPacket::class)
        }
    }

    fun register(listener: Any) {
        val objClass = listener::class
        objClass.declaredMemberFunctions.forEach { function ->
            val annotation = function.findAnnotation<CloudEventListener>()
            if (annotation != null) {
                val eventType = function.parameters.first().type.classifier as KClass<*>
                val handlerMethod = EventHandlerMethod(listener, function, annotation.priority)
                lock.lock()
                try {
                    handlers.getOrPut(eventType) { mutableListOf() }.add(handlerMethod)
                    handlers[eventType]?.sortWith(compareByDescending<EventHandlerMethod> { it.priority })
                }finally {
                    lock.unlock()
                }
            }
        }
    }

    inline fun <reified T : CloudEvent> listen(noinline handler: (T) -> Unit): InlineEventCaller<T> {
        val listener = InlineEventCaller(handler)
        register(listener)
        return listener
    }

    fun <T : CloudEvent> listen(clazz: KClass<T>, handler: (T) -> Unit): InlineEventCaller<T> {
        val listener = InlineEventCaller(handler)
        register(listener)
        return listener
    }

    fun unregister(listener: Any) {
        val objClass = listener::class
        handlers.values.forEach { list ->
            list.removeIf { it.listener == listener }
        }
    }

    fun fireEvent(event: CloudEvent) {
        val fireType = event.fireType
        LOGGER.finest("Firing event ${event::class.simpleName} with fire type $fireType")
        when (event.fireType) {
            EventFireType.GLOBAL -> {
                runBlocking {
                    try {
                        packetManager?.publishAll(
                            CloudEventPacket(
                                gson.toJson(event),
                                event::class.qualifiedName!!,
                                identifier
                            )
                        )
                        fireLocalEvent(event)
                    } catch (e: Exception) {
                        LOGGER.severe(
                            "Error while publishing global event (Make sure ${event::class.simpleName} is serializable)",
                            e
                        )
                    }
                }
                return
            }

            EventFireType.CLIENT -> {
                runBlocking {
                    try {
                        packetManager?.publish(
                            CloudEventPacket(
                                gson.toJson(event),
                                event::class.qualifiedName!!,
                                identifier
                            ), ServiceType.CLIENT
                        )
                        fireLocalEvent(event)
                    } catch (e: Exception) {
                        LOGGER.severe(
                            "Error while publishing client event (Make sure ${event::class.simpleName} is serializable)",
                            e
                        )
                    }
                }
                return
            }

            EventFireType.SERVER -> {
                runBlocking {
                    try {
                        packetManager?.publish(
                            CloudEventPacket(
                                gson.toJson(event),
                                event::class.qualifiedName!!,
                                identifier
                            ), ServiceType.SERVER
                        )
                        fireLocalEvent(event)
                    } catch (e: Exception) {
                        LOGGER.severe(
                            "Error while publishing server event (Make sure ${event::class.simpleName} is serializable)",
                            e
                        )
                    }
                }
                return
            }

            EventFireType.NODE -> {
                runBlocking {
                    try {
                        packetManager?.publish(
                            CloudEventPacket(
                                gson.toJson(event),
                                event::class.qualifiedName!!,
                                identifier
                            ), ServiceType.NODE
                        )
                        fireLocalEvent(event)
                    } catch (e: Exception) {
                        LOGGER.severe(
                            "Error while publishing node event (Make sure ${event::class.simpleName} is serializable)",
                            e
                        )
                    }
                }
                return
            }

            else -> {
                fireLocalEvent(event)
            }
        }
    }

    internal fun fireLocalEvent(event: CloudEvent) {
        val eventType = event::class
        handlers[eventType]?.forEach { handlerMethod ->
            try {
                handlerMethod.function.call(handlerMethod.listener, event)
            } catch (e: Exception) {
                LOGGER.severe("Error while calling event handler", e)
            }
        }
    }

}
package dev.redicloud.event

import dev.redicloud.api.events.*
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.gson.gson
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

class EventManager(
    override val identifier: String,
    val packetManager: PacketManager?
) : IEventManager {

    companion object {
        val LOGGER = LogManager.logger(EventManager::class)
        private val MANAGERS = mutableMapOf<String, EventManager>()

        fun getManager(identifier: String): EventManager? = MANAGERS[identifier]
    }

    val handlers: MutableMap<KClass<*>, MutableList<EventHandlerMethod>> = HashMap()
    val lock = ReentrantLock(true)

    init {
        MANAGERS[identifier] = this
        if (packetManager != null && !packetManager.isPacketRegistered(CloudEventPacket::class)) {
            packetManager.registerPacket(CloudEventPacket::class)
        }
    }

    override fun registerListener(listener: Any) {
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

    override fun registerInlineListener(listener: InlineEventCaller<*>) {
        InlineEventCaller::class.declaredMemberFunctions.forEach { function ->
            val annotation = function.findAnnotation<CloudEventListener>()
            if (annotation != null) {
                val eventType = listener.eventClass
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

    override fun unregisterListener(listener: Any) {
        lock.lock()
        try {
            handlers.values.forEach { list ->
                list.removeIf { it.listener == listener }
            }
        }finally {
            lock.unlock()
        }
    }

    override fun fireEvent(event: CloudEvent) {
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
                            ), ServiceType.MINECRAFT_SERVER
                        )
                        packetManager?.publish(
                            CloudEventPacket(
                                gson.toJson(event),
                                event::class.qualifiedName!!,
                                identifier
                            ), ServiceType.PROXY_SERVER
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

            EventFireType.MINECRAFT_SERVER -> {
                runBlocking {
                    try {
                        packetManager?.publish(
                            CloudEventPacket(
                                gson.toJson(event),
                                event::class.qualifiedName!!,
                                identifier
                            ), ServiceType.MINECRAFT_SERVER
                        )
                        fireLocalEvent(event)
                    } catch (e: Exception) {
                        LOGGER.severe(
                            "Error while publishing minecraft server event (Make sure ${event::class.simpleName} is serializable)",
                            e
                        )
                    }
                }
                return
            }

            EventFireType.PROXY_SERVER -> {
                runBlocking {
                    try {
                        packetManager?.publish(
                            CloudEventPacket(
                                gson.toJson(event),
                                event::class.qualifiedName!!,
                                identifier
                            ), ServiceType.PROXY_SERVER
                        )
                        fireLocalEvent(event)
                    } catch (e: Exception) {
                        LOGGER.severe(
                            "Error while publishing proxy server event (Make sure ${event::class.simpleName} is serializable)",
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
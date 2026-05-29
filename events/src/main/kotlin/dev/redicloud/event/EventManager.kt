package dev.redicloud.event

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.CloudEventListener
import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.events.IEventManager
import dev.redicloud.api.events.InlineEventCaller
import dev.redicloud.api.service.ServiceType
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.gson.gson
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
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

    fun unregister(classLoader: ClassLoader) {
        lock.lock()
        try {
            handlers.values.forEach { list ->
                list.removeIf { it.listener::class.java.classLoader == classLoader }
            }
        } finally {
            lock.unlock()
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
                } finally {
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
                } finally {
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
        } finally {
            lock.unlock()
        }
    }

    override fun fireEvent(event: CloudEvent) {
        LOGGER.finest("Firing event ${event::class.simpleName} with fire type ${event.fireType}")
        when (event.fireType) {
            EventFireType.GLOBAL -> publishEventBroadcast(event)
            EventFireType.CLIENT -> publishEventToServices(event, "client", ServiceType.CLIENT)
            EventFireType.SERVER -> publishEventToServices(
                event,
                "server",
                ServiceType.MINECRAFT_SERVER,
                ServiceType.PROXY_SERVER
            )
            EventFireType.MINECRAFT_SERVER -> publishEventToServices(
                event,
                "minecraft server",
                ServiceType.MINECRAFT_SERVER
            )
            EventFireType.PROXY_SERVER -> publishEventToServices(event, "proxy server", ServiceType.PROXY_SERVER)
            EventFireType.NODE -> publishEventToServices(event, "node", ServiceType.NODE)
            else -> fireLocalEvent(event)
        }
    }

    private fun publishEventBroadcast(event: CloudEvent) {
        runBlocking {
            @Suppress("TooGenericExceptionCaught")
            try {
                packetManager?.publishBroadcast(createEventPacket(event))
                fireLocalEvent(event)
            } catch (e: Exception) {
                LOGGER.severe(
                    "Error while publishing global event (Make sure ${event::class.simpleName} is serializable)",
                    e
                )
            }
        }
    }

    private fun publishEventToServices(event: CloudEvent, description: String, vararg serviceTypes: ServiceType) {
        runBlocking {
            @Suppress("TooGenericExceptionCaught")
            try {
                val packet = createEventPacket(event)
                serviceTypes.forEach { serviceType ->
                    packetManager?.publish(packet, serviceType)
                }
                fireLocalEvent(event)
            } catch (e: Exception) {
                LOGGER.severe(
                    "Error while publishing $description event (Make sure ${event::class.simpleName} is serializable)",
                    e
                )
            }
        }
    }

    private fun createEventPacket(event: CloudEvent): CloudEventPacket {
        return CloudEventPacket(
            gson.toJson(event),
            event::class.qualifiedName!!,
            identifier
        )
    }

    internal fun fireLocalEvent(event: CloudEvent) {
        val eventType = event::class
        lock.withLock {
            handlers[eventType]?.forEach { handlerMethod ->
                @Suppress("TooGenericExceptionCaught")
                try {
                    handlerMethod.function.call(handlerMethod.listener, event)
                } catch (e: Exception) {
                    LOGGER.severe("Error while calling event handler", e)
                }
            }
        }
    }
}

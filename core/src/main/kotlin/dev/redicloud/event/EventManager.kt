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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

/**
 * Central event dispatcher that supports both local and distributed event firing.
 *
 * Events can be published globally, to specific service types, or fired locally only.
 * Distributed events are delivered via [PacketManager] using [CloudEventPacket].
 * Listeners are registered by scanning for [CloudEventListener]-annotated functions.
 *
 * @param identifier a unique identifier for this event manager instance
 * @param packetManager optional packet manager for distributed event delivery; null for local-only
 */
class EventManager(
    override val identifier: String,
    val packetManager: PacketManager?
) : IEventManager {

    companion object {
        /** Logger for the event subsystem. */
        val LOGGER = LogManager.logger(EventManager::class)
        private val MANAGERS = mutableMapOf<String, EventManager>()

        /**
         * Retrieves a registered [EventManager] by its identifier.
         *
         * @param identifier the manager identifier
         * @return the manager instance, or null if not found
         */
        fun getManager(identifier: String): EventManager? = MANAGERS[identifier]
    }

    /** Map of event types to their registered handler methods. */
    val handlers: MutableMap<KClass<*>, MutableList<EventHandlerMethod>> = HashMap()

    /** Fair reentrant lock guarding handler registration and removal. */
    val lock = ReentrantLock(true)

    init {
        MANAGERS[identifier] = this
        if (packetManager != null && !packetManager.isPacketRegistered(CloudEventPacket::class)) {
            packetManager.registerPacket(CloudEventPacket::class)
        }
    }

    /**
     * Removes all event handlers loaded by the given [classLoader].
     *
     * @param classLoader the class loader whose handlers should be unregistered
     */
    fun unregister(classLoader: ClassLoader) {
        lock.withLock {
            handlers.values.forEach { list ->
                list.removeIf { it.listener::class.java.classLoader == classLoader }
            }
        }
    }

    override fun registerListener(listener: Any) {
        listener::class.declaredMemberFunctions.forEach { function ->
            val annotation = function.findAnnotation<CloudEventListener>() ?: return@forEach
            val eventType = function.parameters.first().type.classifier as KClass<*>
            registerHandler(eventType, EventHandlerMethod(listener, function, annotation.priority))
        }
    }

    override fun registerInlineListener(listener: InlineEventCaller<*>) {
        InlineEventCaller::class.declaredMemberFunctions.forEach { function ->
            val annotation = function.findAnnotation<CloudEventListener>() ?: return@forEach
            registerHandler(listener.eventClass, EventHandlerMethod(listener, function, annotation.priority))
        }
    }

    private fun registerHandler(eventType: KClass<*>, handlerMethod: EventHandlerMethod) {
        lock.withLock {
            handlers.getOrPut(eventType) { mutableListOf() }.add(handlerMethod)
            handlers[eventType]?.sortWith(compareByDescending { it.priority })
        }
    }

    override fun unregisterListener(listener: Any) {
        lock.withLock {
            handlers.values.forEach { list ->
                list.removeIf { it.listener == listener }
            }
        }
    }

    override suspend fun fireEvent(event: CloudEvent) {
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

    private suspend fun publishEventBroadcast(event: CloudEvent) {
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

    private suspend fun publishEventToServices(event: CloudEvent, description: String, vararg serviceTypes: ServiceType) {
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

    private fun createEventPacket(event: CloudEvent): CloudEventPacket {
        return CloudEventPacket(
            gson.toJson(event),
            event::class.qualifiedName!!,
            identifier
        )
    }

    internal suspend fun fireLocalEvent(event: CloudEvent) {
        val eventType = event::class
        val handlersCopy = lock.withLock { handlers[eventType]?.toList() } ?: return
        handlersCopy.forEach { handlerMethod ->
            @Suppress("TooGenericExceptionCaught")
            try {
                if (handlerMethod.function.isSuspend) {
                    handlerMethod.function.callSuspend(handlerMethod.listener, event)
                } else {
                    handlerMethod.function.call(handlerMethod.listener, event)
                }
            } catch (e: Exception) {
                LOGGER.severe("Error while calling event handler", e)
            }
        }
    }
}

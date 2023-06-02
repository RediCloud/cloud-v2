package dev.redicloud.event

import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.ServiceType
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

class EventManager(val packetManager: PacketManager?) {

    val handlers: MutableMap<KClass<*>, MutableList<EventHandlerMethod>> = HashMap()

    init {
        packetManager?.registerPacket(CloudEventPacket(object : CloudEvent() {}))
    }

    fun register(listener: Any) {
        val objClass = listener::class
        objClass.declaredMemberFunctions.forEach { function ->
            val annotation = function.findAnnotation<CloudEventListener>()
            if (annotation != null) {
                val eventType = function.parameters.first().type.classifier as KClass<*>
                val handlerMethod = EventHandlerMethod(listener, function, annotation.priority)
                handlers.getOrPut(eventType) { mutableListOf() }.add(handlerMethod)
                handlers[eventType]?.sortWith(compareByDescending<EventHandlerMethod> { it.priority })
            }
        }
    }

    inline fun <reified T : CloudEvent> listen(noinline handler: (T) -> Unit): InlineEventCaller<T> {
        val listener = InlineEventCaller(handler)
        val objClass = InlineEventCaller::class
        objClass.declaredMemberFunctions.forEach { function ->
            val annotation = function.findAnnotation<CloudEventListener>()
            if (annotation != null) {
                val eventType = T::class
                val handlerMethod = EventHandlerMethod(listener, function, annotation.priority)
                handlers.getOrPut(eventType) { mutableListOf() }.add(handlerMethod)
                handlers[eventType]?.sortWith(compareByDescending { it.priority })
            }
        }
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
        when (event.fireType) {
            EventFireType.GLOBAL -> {
                if (!isSerializable(event)) {
                    throw IllegalArgumentException("Event is not serializable and cannot be fired globally")
                }
                runBlocking { packetManager?.publishAll(CloudEventPacket(event)) }
                return
            }
            EventFireType.CLIENT -> {
                if (!isSerializable(event)) {
                    throw IllegalArgumentException("Event is not serializable and cannot be fired to a client")
                }
                runBlocking { packetManager?.publish(CloudEventPacket(event), ServiceType.CLIENT) }
                return
            }
            EventFireType.SERVER -> {
                if (!isSerializable(event)) {
                    throw IllegalArgumentException("Event is not serializable and cannot be fired to a server")
                }
                runBlocking { packetManager?.publish(CloudEventPacket(event), ServiceType.SERVER) }
                return
            }
            EventFireType.NODE -> {
                if (!isSerializable(event)) {
                    throw IllegalArgumentException("Event is not serializable and cannot be fired to a node")
                }
                runBlocking { packetManager?.publish(CloudEventPacket(event), ServiceType.NODE) }
                return
            }
            else -> {
                val eventType = event::class
                handlers[eventType]?.forEach { handlerMethod ->
                    handlerMethod.function.call(handlerMethod.listener, event)
                }
                return
            }
        }
    }

    fun isSerializable(event: CloudEvent): Boolean =
        try { packetManager?.gson?.toJson(event) != null }catch (e: Exception) { false }

}
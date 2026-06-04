package dev.redicloud.packets

/**
 * Serialized envelope for transmitting packets over Redis pub/sub channels.
 *
 * @property data the JSON-serialized packet payload
 * @property clazz the fully-qualified class name of the packet for deserialization
 */
data class PackedPacket(val data: String, val clazz: String)

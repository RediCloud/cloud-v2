package dev.redicloud.service.base.packets.player

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import java.util.*

class CloudPlayerSoundPacket(
    uniqueId: UUID,
    private val soundName: String,
    private val sourceName: String,
    val volume: Float,
    val pitch: Float,
    val stop: Boolean = false
) : CloudPlayerPacket(uniqueId) {

    constructor(uniqueId: UUID, sound: Sound, stop: Boolean = false) : this(
        uniqueId,
        sound.name().asString(),
        sound.source().name,
        sound.volume(),
        sound.pitch(),
        stop
    )

    val key: Key
        get() = Key.key(soundName)

    val source: Sound.Source
        get() = Sound.Source.valueOf(sourceName)

    fun createSound(): Sound {
        return Sound.sound(key, source, volume, pitch)
    }

}
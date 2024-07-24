package dev.redicloud.service.base.packets.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import java.time.Duration
import java.util.UUID

class CloudPlayerTitlePacket(
    uniqueId: UUID,
    private val jsonTitle: String,
    private val jsonSubTitle: String,
    private val fadeIn: Duration,
    private val stay: Duration,
    private val fadeOut: Duration
) : CloudPlayerPacket(uniqueId) {

    constructor(uniqueId: UUID, title: Title) : this(
        uniqueId,
        GsonComponentSerializer.gson().serialize(title.title()),
        GsonComponentSerializer.gson().serialize(title.subtitle()),
        title.times()?.fadeIn() ?: Title.DEFAULT_TIMES.fadeIn(),
        title.times()?.stay() ?: Title.DEFAULT_TIMES.stay(),
        title.times()?.fadeOut() ?: Title.DEFAULT_TIMES.fadeOut()
    )

    private val title: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonTitle)

    private val subTitle: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonSubTitle)

    private val times: Times
        get() = Times.times(fadeIn, stay, fadeOut)

    fun createTitle(): Title {
        return Title.title(title, subTitle, times)
    }

}
package dev.redicloud.service.base.packets.player

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.UUID

class CloudPlayerBossBarPacket(
    uniqueId: UUID,
    private val jsonComponent: String,
    private val colorName: String,
    private val overlayName: String,
    val progress: Float,
    private val flagsName: List<String>,
    val hide: Boolean = false
) : CloudPlayerPacket(uniqueId) {

    constructor(uniqueId: UUID, bossBar: BossBar, hide: Boolean = false) : this(
        uniqueId,
        GsonComponentSerializer.gson().serialize(bossBar.name()),
        bossBar.color().name,
        bossBar.overlay().name,
        bossBar.progress(),
        bossBar.flags().map { it.name }
    )

    private val component: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonComponent)

    private val color: BossBar.Color
        get() = BossBar.Color.valueOf(colorName)

    private val overlay: BossBar.Overlay
        get() = BossBar.Overlay.valueOf(overlayName)

    private val flags: List<BossBar.Flag>
        get() = flagsName.map { BossBar.Flag.valueOf(it) }

    fun createBossBar(): BossBar {
        return BossBar.bossBar(
            this.component,
            this.progress,
            this.color,
            this.overlay,
            flags.toSet()
        )
    }

}
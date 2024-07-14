package dev.redicloud.api.player

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.ICloudServer
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.sound.Sound
import java.util.UUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title

interface ICloudPlayerExecutor {

    suspend fun sendMessage(uniqueId: UUID, component: Component)
    suspend fun sendMessage(cloudPlayer: ICloudPlayer, component: Component)

    suspend fun showBossBar(uniqueId: UUID, bossBar: BossBar)
    suspend fun showBossBar(cloudPlayer: ICloudPlayer, bossBar: BossBar)

    suspend fun hideBossBar(uniqueId: UUID, bossBar: BossBar)
    suspend fun hideBossBar(cloudPlayer: ICloudPlayer, bossBar: BossBar)

    suspend fun playSound(uniqueId: UUID, sound: Sound)
    suspend fun playSound(cloudPlayer: ICloudPlayer, sound: Sound)

    suspend fun stopSound(uniqueId: UUID, sound: Sound)
    suspend fun stopSound(cloudPlayer: ICloudPlayer, sound: Sound)

    suspend fun showTitle(uniqueId: UUID, title: Title)
    suspend fun showTitle(cloudPlayer: ICloudPlayer, title: Title)

    suspend fun showBook(uniqueId: UUID, book: Book)
    suspend fun showBook(cloudPlayer: ICloudPlayer, book: Book)

    suspend fun sendPlayerListHeaderAndFooter(uniqueId: UUID, header: Component, footer: Component)
    suspend fun sendPlayerListHeaderAndFooter(cloudPlayer: ICloudPlayer, header: Component, footer: Component)

    suspend fun sendResourcePacks(uniqueId: UUID, resourcePackRequest: ResourcePackRequest)
    suspend fun sendResourcePacks(cloudPlayer: ICloudPlayer, resourcePackRequest: ResourcePackRequest)

    suspend fun connect(uniqueId: UUID, serviceId: ServiceId)
    suspend fun connect(uniqueId: UUID, serverName: String)
    suspend fun connect(cloudPlayer: ICloudPlayer, serverName: String)
    suspend fun connect(cloudPlayer: ICloudPlayer, serviceId: ServiceId)
    suspend fun connect(cloudPlayer: ICloudPlayer, server: ICloudServer)

    suspend fun kick(uniqueId: UUID, reason: Component)
    suspend fun kick(cloudPlayer: ICloudPlayer, reason: Component)

}
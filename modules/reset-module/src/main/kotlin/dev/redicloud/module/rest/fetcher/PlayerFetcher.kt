package dev.redicloud.module.rest.fetcher

import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerRepository
import java.util.*


class PlayerFetcher(
    private val playerRepository: ICloudPlayerRepository
) {
    suspend fun fetchPlayerByName(name: String?): ICloudPlayer? {
        if (name == null) {
            return null
        }
        return playerRepository.getPlayer(name)
    }

    suspend fun fetchPlayerByUniqueId(id: String?): ICloudPlayer? {
        return try {
            val uuid = UUID.fromString(id)
            playerRepository.getPlayer(uuid)
        }catch (e: IllegalArgumentException) {
            null
        }
    }

}

package dev.redicloud.module.rest.parser

import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerRepository
import java.util.*


class PlayerRestParser(
    private val playerRepository: ICloudPlayerRepository
) {
    suspend fun parseNameToPlayer(handler: () -> String?): ICloudPlayer? {
        val name = handler() ?: return null
        return playerRepository.getPlayer(name)
    }

    suspend fun parseUUIDToPlayer(handler: () -> String?): ICloudPlayer? {
        val uuidString = handler() ?: return null
        return try {
            val uuid = UUID.fromString(uuidString)
            playerRepository.getPlayer(uuid)
        }catch (e: IllegalArgumentException) {
            null
        }
    }

}

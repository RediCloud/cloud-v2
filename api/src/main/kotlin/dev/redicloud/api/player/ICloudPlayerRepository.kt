package dev.redicloud.api.player

import java.util.UUID

interface ICloudPlayerRepository {

    suspend fun getPlayer(uniqueId: UUID): ICloudPlayer?

    suspend fun getPlayer(name: String): ICloudPlayer?

    suspend fun createPlayer(cloudPlayer: ICloudPlayer): ICloudPlayer

    suspend fun updatePlayer(cloudPlayer: ICloudPlayer): ICloudPlayer

    suspend fun deletePlayer(cloudPlayer: ICloudPlayer): Boolean

    suspend fun deletePlayer(uniqueId: UUID): Boolean

    suspend fun existsPlayer(uniqueId: UUID): Boolean

    suspend fun existsPlayer(name: String): Boolean

    suspend fun getRegisteredPlayers(): List<ICloudPlayer>

    suspend fun getConnectedPlayers(): List<ICloudPlayer>

}
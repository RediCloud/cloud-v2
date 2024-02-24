package dev.redicloud.api.provider

import java.util.UUID

interface IServerPlayerProvider {

    fun getConnectedPlayerCount(): Int
    fun getConnectedPlayers(): List<UUID>
    fun getMaxPlayerCount(): Int

}
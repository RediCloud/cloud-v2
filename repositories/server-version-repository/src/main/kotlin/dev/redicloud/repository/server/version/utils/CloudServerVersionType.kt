package dev.redicloud.repository.server.version.utils

enum class CloudServerVersionType {

    PAPER,
    SPIGOT,
    BUKKIT,
    VELOCITY,
    BUNGEECORD,
    WATERFALL,
    FOLIA,
    MINESTOM,
    LIMBO,
    CUSTOM;

    fun isCraftBukkitBased(): Boolean {
        return this == PAPER || this == SPIGOT || this == BUKKIT
    }

}
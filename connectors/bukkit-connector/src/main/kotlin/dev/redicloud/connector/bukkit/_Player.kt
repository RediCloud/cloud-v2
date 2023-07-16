package dev.redicloud.connector.bukkit

import org.bukkit.entity.Player

fun Player.gibMirWasCooles() {
    this.flySpeed = 0.1f
    this.walkSpeed = 0.1f
    this.allowFlight = true
    this.isFlying = true
    this.isGlowing = true
}
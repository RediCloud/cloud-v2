package dev.redicloud.api.commands

/**
 * Interface to represent the actor of a command
 */
interface ICommandActor<T> {

    /**
     * The identifier of the actor. This can be the uuid of a player
     */
    val identifier: T

    /**
     * Checks if the actor has the given permission.
     */
    fun hasPermission(permission: String?): Boolean

    /**
     * Sends a message to the actor
     */
    fun sendMessage(text: String)

    /**
     * Sends a header to the actor
     */
    fun sendHeader(text: String)

}
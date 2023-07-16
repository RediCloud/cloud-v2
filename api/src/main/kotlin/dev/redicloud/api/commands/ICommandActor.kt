package dev.redicloud.api.commands

interface ICommandActor<T> {
    val identifier: T
    fun hasPermission(permission: String?): Boolean

    fun sendMessage(text: String)

    fun sendHeader(text: String)

}
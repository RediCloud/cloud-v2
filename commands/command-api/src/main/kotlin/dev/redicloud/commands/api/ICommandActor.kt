package dev.redicloud.commands.api

interface ICommandActor<T> {
    val identifier: T
    fun hasPermission(permission: String?): Boolean

    fun sendMessage(text: String)

    fun sendHeader(text: String)

}
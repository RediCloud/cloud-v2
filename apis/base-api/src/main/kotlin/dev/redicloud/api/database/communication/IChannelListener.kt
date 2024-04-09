package dev.redicloud.api.database.communication

interface IChannelListener<M> {
    fun onMessage(channel: String, message: M)
}
package dev.redicloud.database.communication

import dev.redicloud.api.database.communication.IChannelListener
import dev.redicloud.api.database.communication.ICommunicationChannel
import dev.redicloud.database.DatabaseConnection

class CommunicationChannel(
    override val name: String,
    databaseConnection: DatabaseConnection
) : ICommunicationChannel {

    private val topic = databaseConnection.client.getTopic(name)
    override val subscribtionCount: Int
        get() {
            return topic.countListeners()
        }

    override suspend fun publish(message: Any) {
        topic.publish(message)
    }

    override suspend fun <M> subscribe(messageClass: Class<M>, listener: IChannelListener<M>): Int {
        return topic.addListener(messageClass) { channel, message ->
            listener.onMessage(channel.toString(), message)
        }
    }

    override suspend fun unsubscribe(listenerId: Int) {
        topic.removeListener(listenerId)
    }

    override suspend fun unsubscribeAll() {
        topic.removeAllListeners()
    }

}
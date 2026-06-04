package dev.redicloud.database.communication

import dev.redicloud.api.database.communication.IChannelListener
import dev.redicloud.api.database.communication.ICommunicationChannel
import dev.redicloud.database.DatabaseConnection

/**
 * Pub/Sub communication channel backed by a Redisson topic.
 *
 * Allows publishing messages and subscribing/unsubscribing listeners
 * on a named Redis topic.
 *
 * @param name the name of the Redis topic
 * @param databaseConnection the database connection providing the Redisson client
 */
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

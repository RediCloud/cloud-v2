package dev.redicloud.api.database.communication

interface ICommunicationChannel {

    val name: String
    val subscribtionCount: Int

    suspend fun publish(message: Any)

    suspend fun <M> subscribe(messageClass: Class<M>, listener: IChannelListener<M>): Int

    suspend fun unsubscribe(listenerId: Int)

    suspend fun unsubscribeAll()

}
package dev.redicloud.api.tasks

import java.util.UUID

interface ICloudTask {

    val id: UUID

    suspend fun execute(): Boolean

    fun isCanceled(): Boolean

    fun isStarted(): Boolean

    fun cancel()

    fun onFinished(block: () -> Unit)
}

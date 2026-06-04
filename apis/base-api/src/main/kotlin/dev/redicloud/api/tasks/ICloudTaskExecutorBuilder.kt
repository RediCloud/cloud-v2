package dev.redicloud.api.tasks

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.packets.AbstractPacket
import kotlin.reflect.KClass
import kotlin.time.Duration

interface ICloudTaskExecutorBuilder {

    fun task(task: ICloudTask): ICloudTaskExecutorBuilder

    fun instant(): ICloudTaskExecutorBuilder

    fun atTime(atTime: Long): ICloudTaskExecutorBuilder

    fun delay(delay: Long): ICloudTaskExecutorBuilder

    fun delay(duration: Duration): ICloudTaskExecutorBuilder

    fun period(period: Duration, maxExecutions: Int = -1): ICloudTaskExecutorBuilder

    fun event(eventClazz: KClass<out CloudEvent>): ICloudTaskExecutorBuilder

    fun packet(packetClazz: KClass<out AbstractPacket>): ICloudTaskExecutorBuilder

    fun register(): ICloudTask
}

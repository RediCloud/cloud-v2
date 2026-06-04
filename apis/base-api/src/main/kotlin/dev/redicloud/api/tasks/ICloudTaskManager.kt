package dev.redicloud.api.tasks

import java.util.UUID

interface ICloudTaskManager {

    fun register(task: ICloudTask): UUID

    fun unregister(id: UUID): ICloudTask?

    fun unregister(task: ICloudTask): ICloudTask?

    fun getTasks(): List<ICloudTask>

    fun builder(): ICloudTaskExecutorBuilder
}

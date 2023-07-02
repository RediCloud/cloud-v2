package dev.redicloud.repository.server.version.task

import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.tasks.CloudTask

class CloudServerVersionUpdateTask(
    private val serverVersionRepository: CloudServerVersionRepository,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) : CloudTask() {

    override suspend fun execute(): Boolean {

        serverVersionRepository.getVersions().forEach {
            if (it.typeId == null) return@forEach
            val type = serverVersionTypeRepository.getType(it.typeId!!) ?: return@forEach
            val handle = IServerVersionHandler.getHandler(type)
            if (handle.isUpdateAvailable(it)) handle.update(it)
        }

        return false
    }

}
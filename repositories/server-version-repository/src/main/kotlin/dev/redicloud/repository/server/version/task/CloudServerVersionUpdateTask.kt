package dev.redicloud.repository.server.version.task

import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.tasks.CloudTask

class CloudServerVersionUpdateTask(
    private val serverVersionRepository: CloudServerVersionRepository,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) : CloudTask() {

    companion object {
        private val logger = LogManager.logger(CloudServerVersionUpdateTask::class)
    }

    override suspend fun execute(): Boolean {

        serverVersionTypeRepository.updateDefaultTypes(this.serverVersionRepository)

        serverVersionRepository.getVersions().forEach {
            if (it.typeId == null) return@forEach
            val type = serverVersionTypeRepository.getType(it.typeId!!) ?: return@forEach
            val handle = IServerVersionHandler.getHandler(type)
            if (handle.isUpdateAvailable(it) && !handle.getLock(it).isLocked) {
                logger.info("Updating server version ${toConsoleValue(it.getDisplayName())}...")
                handle.update(it, type)
            }
        }

        return false
    }

}
package dev.redicloud.repository.server.version.task

import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.version.CloudServerVersionRepository
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.tasks.CloudTask

class CloudServerVersionUpdateTask(
    private val silent: Boolean,
    private val serverVersionRepository: CloudServerVersionRepository,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository
) : CloudTask() {

    companion object {
        private val logger = LogManager.logger(CloudServerVersionUpdateTask::class)
    }

    override suspend fun execute(): Boolean {

        serverVersionTypeRepository.pullOnlineTypes(this.serverVersionRepository, silent)
        serverVersionRepository.pullOnlineVersions(this.serverVersionTypeRepository, silent)

        serverVersionRepository.getVersions().forEach {
            if (it.typeId == null) return@forEach
            val type = serverVersionTypeRepository.getType(it.typeId!!) ?: return@forEach
            val handle = IServerVersionHandler.getHandler(type)
            if (it.used && !handle.getLock(it).isLocked && handle.isUpdateAvailable(it)) {
                if (!silent) logger.info("Updating server version ${toConsoleValue(it.displayName)}...")
                handle.update(it, type)
            }
        }

        return false
    }

}
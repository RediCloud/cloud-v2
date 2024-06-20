package dev.redicloud.server.factory.task

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.api.template.file.ICloudFileTemplateRepository
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.logging.LogManager
import dev.redicloud.server.factory.ServerFactory
import dev.redicloud.tasks.CloudTask

class CloudServerCopyFilesTask(
    private val thisNodeId: ServiceId,
    private val serverFactory: ServerFactory,
    private val serverRepository: ICloudServerRepository,
    private val fileTemplateRepository: ICloudFileTemplateRepository
) : CloudTask() {

    companion object {
        private val logger = LogManager.Companion.logger(CloudServerCopyFilesTask::class)
    }

    override suspend fun execute(): Boolean {
        serverFactory.getCopyQueue().forEach { information ->
            val fileTemplate = fileTemplateRepository.getTemplate(information.templateId)
            if (fileTemplate == null) {
                logger.warning("§cCould not copy file ${toConsoleValue(information.path, false)} for server ${toConsoleValue(information.serviceId.toName(), false)} to template ${toConsoleValue(information.templateId, false)} because the template does not exist")
                serverFactory.copyQueue.remove(information)
                return@forEach
            }
            val server = serverRepository.getServer<ICloudServer>(information.serviceId)
            if (server == null) {
                logger.warning("§cCould not copy file ${toConsoleValue(information.path, false)} for server ${toConsoleValue(information.serviceId.toName(), false)} to template ${toConsoleValue(fileTemplate.displayName, false)} because the server does not exist")
                serverFactory.copyQueue.remove(information)
                return@forEach
            }
            if (server.hostNodeId != thisNodeId) {
                return@forEach
            }
            val process =
                serverFactory.hostedProcesses.firstOrNull { it.serverId == server.serviceId } ?: return@forEach
            val folder = process.fileCopier.workDirectory
            val file = folder.resolve(information.path)
            if (!file.exists()) {
                logger.warning("§cCould not copy file ${toConsoleValue(information.path, false)} for server ${toConsoleValue(server.serviceId.toName(), false)} to template ${toConsoleValue(fileTemplate.displayName, false)} because it does not exist")
                serverFactory.copyQueue.remove(information)
                return@forEach
            }
            val templateFolder = fileTemplate.folder
            serverFactory.copyQueue.remove(information)
            if (file.isFile) {
                val templateFile = templateFolder.resolve(information.path)
                if (!templateFile.parentFile.exists()) {
                    templateFile.parentFile.mkdirs()
                }
                file.copyTo(templateFile, true)
                logger.info("§aCopied file ${toConsoleValue(information.path, false)} for server ${toConsoleValue(server.serviceId.toName(), false)} to template ${toConsoleValue(fileTemplate.displayName, false)}")
            } else {
                val templateFolder = templateFolder.resolve(information.path)
                if (!templateFolder.exists()) {
                    templateFolder.mkdirs()
                }
                file.copyRecursively(templateFolder, true)
                logger.info("§aCopied folder ${toConsoleValue(information.path, false)} for server ${toConsoleValue(server.serviceId.toName(), false)} to template ${toConsoleValue(fileTemplate.displayName, false)}")
            }
        }
        return false
    }

}
package dev.redicloud.server.factory

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.version.CloudServerVersion
import dev.redicloud.repository.server.version.ServerVersionRepository
import dev.redicloud.repository.server.version.handler.IServerVersionHandler
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.repository.template.file.FileTemplateRepository
import dev.redicloud.utils.STATIC_FOLDER
import dev.redicloud.utils.TEMP_SERVER_FOLDER
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*


class FileCopier(
    serverProcess: ServerProcess,
    cloudServer: CloudServer,
    serverVersionRepository: ServerVersionRepository,
    fileTemplateRepository: FileTemplateRepository
) {

    val serverUniqueId = UUID.randomUUID()
    val serviceId = ServiceId(serverUniqueId, ServiceType.SERVER)
    val configurationTemplate = serverProcess.configurationTemplate
    val serverVersion: CloudServerVersion
    val templates: MutableList<FileTemplate> = mutableListOf()
    val workDirectory: File
    private val logger = LogManager.logger(FileCopier::class)

    init {
        serverVersion = runBlocking { serverVersionRepository.getVersion(configurationTemplate.serverVersionId) }
            ?: throw Exception("Server version ${configurationTemplate.serverVersionId} not found!")
        configurationTemplate.fileTemplateIds.forEach {
            val template = runBlocking { fileTemplateRepository.getTemplate(it) } ?: throw Exception("Template $it not found!")
            templates.add(template)
        }
        workDirectory = if(configurationTemplate.static) {
            File(STATIC_FOLDER.getFile().absolutePath, serverUniqueId.toString())
        }else {
            File(TEMP_SERVER_FOLDER.getFile().absolutePath, serverUniqueId.toString())
        }
        workDirectory.mkdirs()
    }

    suspend fun copyVersionFiles() {
        logger.fine("Copying files for $serviceId of version ${serverVersion.name}")
        val versionHandler = IServerVersionHandler.getHandler(serverVersion.type)
        if (versionHandler.isUpdateAvailable(serverVersion)) {
            versionHandler.update(serverVersion)
        }
        if (!versionHandler.isPatched(serverVersion)) {
            versionHandler.patch(serverVersion)
        }
        versionHandler.getFolder(serverVersion).copyRecursively(workDirectory)
    }

    suspend fun copyTemplates() {
        logger.fine("Copying templates for $serviceId")
        templates.forEach {
            it.getFolder().copyRecursively(workDirectory)
        }
    }

    suspend fun editFiles() {
        //TODO set port, server name, etc.
    }

}
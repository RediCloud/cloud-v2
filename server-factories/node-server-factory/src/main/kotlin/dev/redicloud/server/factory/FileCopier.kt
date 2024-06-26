package dev.redicloud.server.factory

import dev.redicloud.logging.LogManager
import dev.redicloud.repository.server.CloudServer
import dev.redicloud.repository.server.version.CloudServerVersionTypeRepository
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.server.factory.utils.StartDataSnapshot
import dev.redicloud.api.utils.CONNECTORS_FOLDER
import dev.redicloud.api.utils.STATIC_FOLDER
import dev.redicloud.api.utils.TEMP_SERVER_FOLDER
import dev.redicloud.utils.JarView
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.concurrent.withLock


class FileCopier(
    serverProcess: ServerProcess,
    cloudServer: CloudServer,
    private val serverVersionTypeRepository: CloudServerVersionTypeRepository,
    fileTemplateRepository: AbstractFileTemplateRepository,
    private val snapshot: StartDataSnapshot
) {

    companion object {
        private val logger = LogManager.logger(FileCopier::class)
    }

    val serviceId = cloudServer.serviceId
    val configurationTemplate = serverProcess.configurationTemplate
    val templates: List<FileTemplate>
    val workDirectory: File

    init {
        // get templates by given configuration template and collect also inherited templates
        templates = configurationTemplate.fileTemplateIds.mapNotNull { runBlocking { fileTemplateRepository.getTemplate(it) } }
            .flatMap { runBlocking { fileTemplateRepository.collectTemplates(it) } }
        // create work directory
        workDirectory = if(configurationTemplate.static) {
            File(STATIC_FOLDER.getFile().absolutePath, "${cloudServer.name}-${serviceId.id}")
        }else {
            File(TEMP_SERVER_FOLDER.getFile().absolutePath, "${cloudServer.name}-${serviceId.id}")
        }
        if (!workDirectory.exists()) workDirectory.mkdirs()
    }

    suspend fun deleteConnectors() {
        logger.fine("Deleting connectors for $serviceId")
        val pluginFolder = File(workDirectory, snapshot.versionType.connectorFolder)
        if (!pluginFolder.exists()) return
        val plugins = pluginFolder.listFiles()?.filter { it.isFile }?.filter { it.extension == "jar" } ?: return
        plugins.forEach { jar ->
            val jarView = JarView(jar)
            if (!jarView.hasEntry("redicloud.properties")) return@forEach
            jarView.close()
            jar.delete()
        }
    }

    suspend fun copyConnector() {
        logger.fine("Copying connector for $serviceId")
        serverVersionTypeRepository.getLock(snapshot.versionType).withLock {
            CONNECTORS_FOLDER.createIfNotExists()
            val connectorFile = snapshot.versionType.getParsedConnectorFile(true)
            if (!connectorFile.exists()) {
                if (snapshot.versionType.connectorDownloadUrl == null) {
                    logger.warning("Connector download url for ${snapshot.versionType.name} is not set! The server will not connect to the cloud cluster!")
                    logger.warning("You can set the connector download url in the server version type settings with: 'svt edit <name> connector url <url>'")
                    return
                }
                try {
                    runBlocking { serverVersionTypeRepository.downloadConnector(snapshot.versionType, lock = false) }
                    if (!connectorFile.exists()) {
                        logger.warning("Connector file for ${snapshot.versionType.name} does not exist! The server will not connect to the cloud cluster!")
                        logger.warning("You can set the connector file in the server version type settings with: 'svt edit <name> connector jar <connector>'")
                        return
                    }
                }catch (e: Exception) {
                    logger.warning("Failed to download connector for ${snapshot.versionType.name} from ${snapshot.versionType.getParsedConnectorURL().toExternalForm()}", e)
                    logger.warning("The server will not connect to the cloud cluster!")
                    logger.warning("You can set the connector download url in the server version type settings with: 'svt edit <name> connector url <url>'")
                    return
                }
            }
            val pluginFolder = File(workDirectory, snapshot.versionType.connectorFolder)
            if (!pluginFolder.exists()) pluginFolder.mkdirs()
            connectorFile.copyTo(File(pluginFolder, connectorFile.name), overwrite = true)
        }
    }

    /**
     * Copies all files for the server version to the work directory
     */
    suspend fun copyVersionFiles(force: Boolean = true, action: (String) -> String = { it }) {
        logger.fine("Copying files for $serviceId of version ${snapshot.version.displayName}")
        val versionHandler = IServerVersionHandler.getHandler(snapshot.versionType)
        versionHandler.getLock(snapshot.version).withLock {
            runBlocking {
                if (!versionHandler.isPatched(snapshot.version) && versionHandler.isPatchVersion(snapshot.version)) {
                    versionHandler.patch(snapshot.version, lock = false)
                }else if(!versionHandler.isDownloaded(snapshot.version)) {
                    versionHandler.download(snapshot.version, lock = false)
                }
                if (force && configurationTemplate.static || !configurationTemplate.static) {
                    versionHandler.getFolder(snapshot.version).copyRecursively(workDirectory)
                }else {
                    val jar = versionHandler.getJar(snapshot.version)
                    if (jar.exists()) {
                        jar.copyTo(File(workDirectory, jar.name), overwrite = true)
                    }
                }
                snapshot.versionType.doFileEdits(workDirectory, action)
                snapshot.version.doFileEdits(workDirectory, action)
            }
        }
    }

    /**
     * Copies all templates to the work directory
     */
    suspend fun copyTemplates(force: Boolean = true) {
        if (!force && configurationTemplate.static) return
        logger.fine("Copying templates for $serviceId")
        templates.forEach {
            it.folder.copyRecursively(workDirectory, overwrite = false)
        }
    }

}
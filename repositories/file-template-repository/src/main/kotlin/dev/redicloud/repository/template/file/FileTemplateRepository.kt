package dev.redicloud.repository.template.file

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import dev.redicloud.cluster.file.FileCluster
import dev.redicloud.cluster.file.FileCluster.Companion.LOGGER
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.utils.*
import dev.redicloud.utils.service.ServiceId
import java.io.File
import java.util.UUID

class FileTemplateRepository(
    databaseConnection: DatabaseConnection,
    val nodeRepository: NodeRepository,
    private val fileCluster: FileCluster
) :
    DatabaseBucketRepository<FileTemplate>(databaseConnection, "file-template") {

    suspend fun getTemplate(uniqueId: UUID): FileTemplate? {
        return getHandle(uniqueId.toString()).get()
    }

    suspend fun getTemplate(displayName: String): FileTemplate? {
        return getTemplates().firstOrNull { it.getDisplayName().lowercase() == displayName.lowercase() }
    }

    suspend fun getTemplate(name: String, prefix: String): FileTemplate? {
        return getTemplates().firstOrNull { it.name.lowercase() == name.lowercase() && it.prefix.lowercase() == prefix.lowercase() }
    }

    suspend fun existsTemplate(uniqueId: UUID, prefix: String): Boolean {
        return getHandle(uniqueId.toString()).isExists
    }

    suspend fun existsTemplate(displayName: String): Boolean {
        return getTemplates().any { it.getDisplayName().lowercase() == displayName.lowercase() }
    }

    suspend fun existsTemplate(name: String, prefix: String): Boolean {
        return getTemplates().any { it.name.lowercase() == name.lowercase() && it.prefix.lowercase() == prefix.lowercase() }
    }

    suspend fun deleteTemplate(uniqueId: UUID) {
        val templates = getTemplates()
        val template = templates.firstOrNull { it.uniqueId == uniqueId } ?: throw Exception("Template $uniqueId not found!")
        templates.filter { it.inherited.contains(template.uniqueId) }.forEach {
            it.inherited.remove(template.uniqueId)
            updateTemplate(it)
        }
        delete(uniqueId.toString())
        if (template.getFolder().exists() && template.getFolder().isDirectory) {
            template.getFolder().deleteRecursively()
            nodeRepository.getConnectedNodes().forEach {
                pushTemplates(it.serviceId, this.fileCluster)
            }
        }
    }

    suspend fun updateTemplate(template: FileTemplate): FileTemplate {
        val storedTemplate = getTemplate(template.uniqueId) ?: throw Exception("Template ${template.uniqueId} not found!")
        getHandle(template.uniqueId.toString()).set(template)
        if (storedTemplate.getDisplayName() != template.getDisplayName()) {
            val folder = storedTemplate.getFolder()
            if (template.getFolder().exists() && template.getFolder().isDirectory && folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach {
                    it.copyRecursively(template.getFolder().resolve(it.name), true)
                }
                template.getFolder().deleteRecursively()
            } else {
                template.getFolder().deleteRecursively()
                folder.renameTo(template.getFolder())
            }
            nodeRepository.getConnectedNodes().forEach {
                pushTemplates(it.serviceId, this.fileCluster)
            }
        }
        return template
    }

    suspend fun createTemplate(template: FileTemplate): FileTemplate {
        getHandle(template.uniqueId.toString()).set(template)
        nodeRepository.getConnectedNodes().forEach {
            pushTemplates(it.serviceId, this.fileCluster)
        }
        return template
    }

    suspend fun getTemplates(): List<FileTemplate> = getAll()

    suspend fun collectTemplates(
        vararg templates: FileTemplate
    ): List<FileTemplate> {
        val collectedTemplates = mutableListOf<FileTemplate>()
        templates.forEach { fileTemplate ->
            val template = getTemplate(fileTemplate.uniqueId)
                ?: throw Exception("Template ${fileTemplate.uniqueId} not found!")
            collectedTemplates.add(template)
            collectedTemplates.addAll(
                collectTemplates(
                    *template.inherited.mapNotNull { getTemplate(it) }.toTypedArray()
                )
            )
        }
        return collectedTemplates
    }

    suspend fun pushTemplates(serviceId: ServiceId, fileCluster: FileCluster) {
        val node = nodeRepository.getNode(serviceId) ?: return
        LOGGER.info("Pushing templates to ${node.getIdentifyingName()}...")
        var session: Session? = null
        var sftpChannel: ChannelSftp? = null
        try {
            session = fileCluster.createSession(node.serviceId)
            sftpChannel = fileCluster.openChannel(session)

            val id = UUID.randomUUID()
            val workFolder = File(TEMP_FILE_TRANSFER_FOLDER.getFile().absolutePath, id.toString())
            workFolder.mkdirs()
            val zip = File(workFolder, "data.zip")
            zipFile(TEMPLATE_FOLDER.getFile().absolutePath, zip.absolutePath)

            fileCluster.mkdirs(sftpChannel, toUniversalPath(workFolder))

            fileCluster.shareFile(sftpChannel, zip, toUniversalPath(workFolder), "data.zip")
            fileCluster.unzip(node.serviceId, toUniversalPath(zip), toUniversalPath(STORAGE_FOLDER.getFile()))
            fileCluster.deleteFolderRecursive(sftpChannel, toUniversalPath(workFolder))

            workFolder.deleteRecursively()

            LOGGER.info("Successfully pushed templates to ${node.getIdentifyingName()}!")
        }finally {
            sftpChannel?.disconnect()
            session?.disconnect()
        }
    }
}
package dev.redicloud.service.node.repository.template.file

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import dev.redicloud.cluster.file.FileCluster
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.utils.*
import dev.redicloud.utils.service.ServiceId
import java.io.File
import java.util.*

class NodeFileTemplateRepository(
    databaseConnection: DatabaseConnection,
    nodeRepository: NodeRepository,
    private val fileCluster: FileCluster
) : AbstractFileTemplateRepository(databaseConnection, nodeRepository) {

    override suspend fun pushTemplates(serviceId: ServiceId) {
        val node = nodeRepository.getNode(serviceId) ?: return
        FileCluster.LOGGER.info("Pushing templates to ${node.getIdentifyingName()}...")
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

            FileCluster.LOGGER.info("Successfully pushed templates to ${node.getIdentifyingName()}!")
        }finally {
            sftpChannel?.disconnect()
            session?.disconnect()
        }
    }

    override suspend fun updateTemplate(template: FileTemplate): FileTemplate {
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
                pushTemplates(it.serviceId)
            }
        }
        return template
    }

}
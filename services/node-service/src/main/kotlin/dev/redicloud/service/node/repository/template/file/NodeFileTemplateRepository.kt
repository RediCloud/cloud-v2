package dev.redicloud.service.node.repository.template.file

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import dev.redicloud.api.template.file.ICloudFileTemplate
import dev.redicloud.api.utils.STORAGE_FOLDER
import dev.redicloud.api.utils.TEMPLATE_FOLDER
import dev.redicloud.api.utils.TEMP_FILE_TRANSFER_FOLDER
import dev.redicloud.api.utils.toUniversalPath
import dev.redicloud.cluster.file.FileCluster
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.repository.template.file.AbstractFileTemplateRepository
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.utils.*
import dev.redicloud.api.service.ServiceId
import java.io.File
import java.util.*

class NodeFileTemplateRepository(
    private val databaseConnection: DatabaseConnection,
    private val nodeRepository: NodeRepository,
    private val fileCluster: FileCluster,
    packetManager: PacketManager
) : AbstractFileTemplateRepository(databaseConnection, nodeRepository, packetManager) {

    override suspend fun pushTemplates(serviceId: ServiceId) {
        val node = nodeRepository.getNode(serviceId) ?: return
        if (node.serviceId == databaseConnection.serviceId) {
            FileCluster.LOGGER.info("Skipping pushing templates to ${node.identifyName()} because it is the current node!")
            return
        }
        FileCluster.LOGGER.info("Pushing templates to ${node.identifyName()}...")
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
            fileCluster.deleteFolderRecursive(sftpChannel, toUniversalPath(TEMPLATE_FOLDER.getFile()))
            val response = fileCluster.unzip(node.serviceId, toUniversalPath(zip), toUniversalPath(STORAGE_FOLDER.getFile()))
            if (response == null) {
                FileCluster.LOGGER.warning("Unzip process of template pushing does not respond!")
            }
            fileCluster.deleteFolderRecursive(sftpChannel, toUniversalPath(workFolder))

            workFolder.deleteRecursively()

            FileCluster.LOGGER.info("Successfully pushed templates to ${node.identifyName()}!")
        }finally {
            sftpChannel?.disconnect()
            session?.disconnect()
        }
    }

    override suspend fun updateTemplate(template: ICloudFileTemplate): FileTemplate {
        val storedTemplate = getTemplate(template.uniqueId) ?: throw Exception("Template ${template.uniqueId} not found!")
        return set(template.uniqueId.toString(), template).apply {
            if (storedTemplate.displayName != template.displayName) {
                val folder = storedTemplate.folder
                if (template.folder.exists() && template.folder.isDirectory && folder.exists() && folder.isDirectory) {
                    folder.listFiles()?.forEach {
                        it.copyRecursively(template.folder.resolve(it.name), true)
                    }
                    template.folder.deleteRecursively()
                } else {
                    template.folder.deleteRecursively()
                    folder.renameTo(template.folder)
                }
                nodeRepository.getConnectedNodes().forEach {
                    pushTemplates(it.serviceId)
                }
            }
        }
    }

}
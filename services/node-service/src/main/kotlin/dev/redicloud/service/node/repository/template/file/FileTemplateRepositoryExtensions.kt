package dev.redicloud.service.node.repository.template.file

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import dev.redicloud.cluster.file.FileCluster
import dev.redicloud.repository.template.file.FileTemplate
import dev.redicloud.repository.template.file.FileTemplateRepository
import dev.redicloud.service.node.repository.node.LOGGER
import dev.redicloud.utils.*
import dev.redicloud.utils.service.ServiceId
import java.io.File
import java.util.*

suspend fun FileTemplateRepository.pushTemplates(serviceId: ServiceId, fileCluster: FileCluster) {
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
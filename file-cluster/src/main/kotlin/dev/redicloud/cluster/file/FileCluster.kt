package dev.redicloud.cluster.file

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import dev.redicloud.api.events.internal.node.file.FileNodeConnectedEvent
import dev.redicloud.api.events.internal.node.file.FileNodeDisconnectedEvent
import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.utils.CLOUD_PATH
import dev.redicloud.cluster.file.filter.IPFilter
import dev.redicloud.cluster.file.packet.UnzipPacket
import dev.redicloud.cluster.file.packet.UnzipResponse
import dev.redicloud.cluster.file.utils.generatePassword
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.utils.findFreePort
import dev.redicloud.utils.isPortFree
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Paths
import java.util.logging.Filter
import java.util.logging.Level
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FileCluster(
    val serviceId: ServiceId,
    val hostname: String,
    val fileNodeRepository: FileNodeRepository,
    val packetManager: PacketManager,
    val nodeRepository: NodeRepository,
    val eventManager: EventManager
) {

    companion object {
        val LOGGER = LogManager.logger(FileCluster::class)
        private const val PASSWORD_LENGTH = 32
        private const val SFTP_PORT_RANGE_START = 4000
        private const val SFTP_PORT_RANGE_END = 5000
        private val UNZIP_DELAY_MS = 500.milliseconds
    }

    private val ipFilter = IPFilter(this.eventManager, this.fileNodeRepository)
    private var sshd: SshServer? = null
    private val jsch = JSch()
    var port = -1
        private set

    init {
        LogManager.rootLogger().filter = Filter { record
            ->
            record.level != Level.INFO && !record.message.contains("org.apache.sshd")
        }
        packetManager.registerPacket(UnzipPacket::class)
        packetManager.registerPacket(UnzipResponse::class)
    }

    suspend fun connect() {
        val thisNode = if (this.fileNodeRepository.existsFileNode(serviceId)) {
            fileNodeRepository.getFileNode(serviceId)!!
        } else {
            val nodeInternal = this.nodeRepository.getNode(serviceId) != null
            val newFileNode = FileNode(
                this.fileNodeRepository.migrateId(serviceId),
                -1,
                hostname,
                "redicloud",
                generatePassword(PASSWORD_LENGTH),
                nodeInternal,
                CLOUD_PATH
            )
            fileNodeRepository.createFileNode(newFileNode)
        }
        this.port = generatePort(thisNode)

        sshd = SshServer.setUpDefaultServer()
        sshd!!.host = if (hostname.startsWith("[") && hostname.endsWith("]")) {
            hostname.substring(1, hostname.length - 1)
        } else {
            hostname
        }
        sshd!!.port = port

        val cloudPath = Paths.get(CLOUD_PATH)
        sshd!!.fileSystemFactory = VirtualFileSystemFactory(cloudPath)
        sshd!!.subsystemFactories = listOf(SftpSubsystemFactory())

        sshd!!.passwordAuthenticator = PasswordAuthenticator { username, password, session ->
            runBlocking {
                val node = fileNodeRepository.getFileNode(serviceId) ?: return@runBlocking false
                val hostname = when (val clientAddress = session.clientAddress) {
                    is SshdSocketAddress -> {
                        clientAddress.hostName
                    }

                    is InetSocketAddress -> {
                        clientAddress.hostName
                    }

                    else -> {
                        LOGGER.warning(
                            "Unknown client address type tried to connect to the file cluster: " +
                                "${clientAddress::class.simpleName}"
                        )
                        return@runBlocking false
                    }
                }
                return@runBlocking node.connected &&
                    ipFilter.canConnect(hostname) &&
                    username == node.username &&
                    password == node.password
            }
        }
        sshd!!.publickeyAuthenticator = PublickeyAuthenticator { username, key, session ->
            username == "redicloud"
        }
        sshd!!.keyPairProvider = SimpleGeneratorHostKeyProvider()

        sshd!!.start()

        thisNode.startSession(hostname)
        thisNode.connected = true
        fileNodeRepository.updateFileNode(thisNode)
        eventManager.fireEvent(FileNodeConnectedEvent(thisNode.serviceId))
    }

    private suspend fun generatePort(fileNode: FileNode): Int {
        if (System.getProperty("redicloud.filecluster.port") != null) {
            fileNode.port = System.getProperty("redicloud.filecluster.port").toInt()
            fileNodeRepository.updateFileNode(fileNode)
            return fileNode.port
        }
        val range = SFTP_PORT_RANGE_START..SFTP_PORT_RANGE_END
        val port = if (range.contains(fileNode.port) && isPortFree(fileNode.port)) {
            fileNode.port
        } else {
            val newPort = findFreePort(range)
            check(range.contains(newPort)) { "Port $newPort is not in range $range!" }
            fileNode.port = newPort
            fileNodeRepository.updateFileNode(fileNode)
            newPort
        }
        check(port != -1) { "No free port found for file cluster!" }
        return port
    }

    suspend fun disconnect(immediately: Boolean) {
        if (sshd == null || !sshd!!.isStarted) return
        val thisNode = fileNodeRepository.getFileNode(serviceId) ?: error(
            "This file node is not registered in the file cluster!"
        )
        sshd!!.stop(immediately)
        thisNode.endSession()
        thisNode.connected = false
        fileNodeRepository.shutdownAction.run()
        fileNodeRepository.updateFileNode(thisNode)
        eventManager.fireEvent(FileNodeDisconnectedEvent(thisNode.serviceId))
    }

    suspend fun createSession(serviceId: ServiceId): Session {
        val fileNode = fileNodeRepository.getFileNode(serviceId) ?: error(
            "File node with service id $serviceId is not registered in the file cluster!"
        )
        val session = jsch.getSession(fileNode.username, fileNode.hostname, fileNode.port)
        session.setPassword(fileNode.password)
        session.setConfig("serviceId", serviceId.toName())
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect()
        check(session.isConnected) { "Session is not connected!" }
        return session
    }

    suspend fun openChannel(session: Session): ChannelSftp {
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect()
        check(channel.isConnected) { "Channel is not connected!" }
        return channel
    }

    suspend fun mkdirs(channel: ChannelSftp, file: String) {
        val directories = file.split(File.separator)
        var currentPath = ""

        for (directory in directories) {
            if (directory.isNotEmpty()) {
                currentPath += "${File.separator}$directory"
                try {
                    channel.mkdir(currentPath)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun deleteFolderRecursive(channelSftp: ChannelSftp, filePath: String) {
        val path = parsePath(filePath)
        val files = channelSftp.ls(path).map { it as LsEntry }
        for (file in files) {
            val fileName = file.filename
            if (fileName != "." && fileName != "..") {
                val filePath = if (path.endsWith("/")) path + fileName else "$path/$fileName"
                if (file.attrs.isDir) {
                    deleteFolderRecursive(channelSftp, filePath)
                } else {
                    channelSftp.rm(filePath)
                }
            }
        }
        channelSftp.rmdir(path)
    }

    suspend fun shareFile(channel: ChannelSftp, file: File, destinationFolder: String, fileName: String) {
        channel.cd(channel.home)
        channel.cd(parsePath(destinationFolder))
        channel.put(parsePath(file.absolutePath), fileName)
        channel.cd(channel.home)
    }

    suspend fun unzip(serviceId: ServiceId, file: String, unzipPath: String): AbstractPacket? {
        val response = packetManager.publish(
            UnzipPacket(file, unzipPath),
            serviceId
        ).withTimeOut(60.seconds).waitBlocking()
        if (response != null) delay(UNZIP_DELAY_MS)
        return response
    }

    suspend fun requestFile(channel: ChannelSftp, targetFile: String, destinationFile: File): File {
        val serviceId = ServiceId.fromString(
            channel.session.getConfig("serviceId")!!
        )
        val fileNode = fileNodeRepository.getFileNode(serviceId) ?: error(
            "File node with service id $serviceId is not registered in the file cluster!"
        )
        channel.get(parsePath(targetFile), parsePath(destinationFile.absolutePath))
        return destinationFile
    }

    private fun parsePath(path: String): String {
        var newPath = path
        val separator = File.separator
        newPath = newPath.replace(CLOUD_PATH, "")
        newPath = newPath.replace(separator, "/")
        return newPath
    }
}

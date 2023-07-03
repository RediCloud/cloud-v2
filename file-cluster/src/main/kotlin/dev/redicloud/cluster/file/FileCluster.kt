package dev.redicloud.cluster.file

import com.jcraft.jsch.*
import com.jcraft.jsch.ChannelSftp.LsEntry
import dev.redicloud.cluster.file.event.FileNodeConnectedEvent
import dev.redicloud.cluster.file.event.FileNodeDisconnectedEvent
import dev.redicloud.cluster.file.filter.IPFilter
import dev.redicloud.cluster.file.packet.UnzipPacket
import dev.redicloud.cluster.file.packet.UnzipResponse
import dev.redicloud.cluster.file.utils.generatePassword
import dev.redicloud.event.EventManager
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.AbstractPacket
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.utils.*
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.runBlocking
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.common.keyprovider.FileHostKeyCertificateProvider
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileWriter
import java.math.BigInteger
import java.net.InetSocketAddress
import java.nio.file.Paths
import java.security.*
import java.security.KeyPair
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.logging.Filter
import java.util.logging.Level
import kotlin.time.Duration.Companion.seconds


class FileCluster(
    val hostname: String,
    val fileNodeRepository: FileNodeRepository,
    val packetManager: PacketManager,
    val nodeRepository: NodeRepository,
    val eventManager: EventManager
) {

    companion object {
        val LOGGER = LogManager.logger(FileCluster::class)
    }

    private val ipFilter = IPFilter(this.eventManager, this.fileNodeRepository)
    private var sshd: SshServer? = null
    private val jsch = JSch()
    var port = -1

    init {
        LogManager.rootLogger().filter = Filter { record
            -> record.level != Level.INFO && !record.message.contains("org.apache.sshd") }
        packetManager.registerPacket(UnzipPacket::class)
        packetManager.registerPacket(UnzipResponse::class)
    }

    suspend fun connect() {
        val thisNode = if (this.fileNodeRepository.existsFileNode(this.fileNodeRepository.serviceId)) {
            fileNodeRepository.getFileNode(this.fileNodeRepository.serviceId)!!
        } else {
            val nodeInternal = this.nodeRepository.getNode(this.nodeRepository.serviceId) != null
            val newFileNode = FileNode(
                this.fileNodeRepository.migrateId(this.nodeRepository.serviceId),
                -1, hostname,
                "redicloud", generatePassword(32),
                nodeInternal,
                CLOUD_PATH
            )
            fileNodeRepository.createFileNode(newFileNode)
        }
        this.port = generatePort(thisNode)

        sshd = SshServer.setUpDefaultServer()
        sshd!!.host = hostname
        sshd!!.port = port

        val cloudPath = Paths.get(CLOUD_PATH)
        sshd!!.fileSystemFactory = VirtualFileSystemFactory(cloudPath)
        sshd!!.subsystemFactories = listOf(SftpSubsystemFactory())


        sshd!!.passwordAuthenticator = PasswordAuthenticator { username, password, session ->
            runBlocking {
                val node = fileNodeRepository.getFileNode(fileNodeRepository.serviceId) ?: return@runBlocking false
                val clientAddress = session.clientAddress
                val hostname = when (clientAddress) {
                    is SshdSocketAddress -> {
                        clientAddress.hostName
                    }

                    is InetSocketAddress -> {
                        clientAddress.hostName
                    }

                    else -> {
                        LOGGER.warning("Unknown client address type tried to connect to the file cluster: ${clientAddress::class.simpleName}")
                        return@runBlocking false
                    }
                }
                return@runBlocking node.isConnected()
                        && ipFilter.canConnect(hostname)
                        && username == node.username
                        && password == node.password
            }
        }
        sshd!!.publickeyAuthenticator = PublickeyAuthenticator { username, key, session ->
            username == "redicloud"
        }
        sshd!!.keyPairProvider = SimpleGeneratorHostKeyProvider()

        sshd!!.start()

        thisNode.startSession(hostname)
        thisNode.connected = true
        runBlocking { fileNodeRepository.updateFileNode(thisNode) }
        eventManager.fireEvent(FileNodeConnectedEvent(thisNode.serviceId))
    }

    private fun generatePort(fileNode: FileNode): Int {
        if (System.getProperty("redicloud.filecluster.port") != null) {
            fileNode.port = System.getProperty("redicloud.filecluster.port").toInt()
            runBlocking { fileNodeRepository.updateFileNode(fileNode) }
            return fileNode.port
        }
        val range = 4000..5000
        val port = if (range.contains(fileNode.port) && isPortFree(fileNode.port)) {
            fileNode.port
        } else {
            val newPort = findFreePort(range)
            if (!range.contains(newPort)) throw IllegalStateException("Port $newPort is not in range $range!")
            fileNode.port = newPort
            runBlocking { fileNodeRepository.updateFileNode(fileNode) }
            newPort
        }
        if (port == -1) throw IllegalStateException("No free port found for file cluster!")
        return port
    }

    private fun generateKey(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    private fun saveCertificateToFile(certificate: X509Certificate, file: File) {
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        if (file.exists()) file.delete()
        file.createNewFile()
        val pemWriter = JcaPEMWriter(FileWriter(file))
        pemWriter.writeObject(certificate)
        pemWriter.close()
    }

    private fun signCertificate(publicKey: PublicKey, privateKey: PrivateKey): X509Certificate {
        val subject = X500Name("CN=Self-Signed")

        val now = Instant.now()
        val expirationTime = now.plus(31*3, ChronoUnit.DAYS)

        val serialNumber = BigInteger.valueOf(now.toEpochMilli())

        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.encoded)

        val builder = X509v3CertificateBuilder(
            subject,
            serialNumber,
            Date.from(now),
            Date.from(expirationTime),
            subject,
            publicKeyInfo
        )

        val contentSigner: ContentSigner = JcaContentSignerBuilder("SHA256WithRSA").build(privateKey)
        val certificateHolder: X509CertificateHolder = builder.build(contentSigner)

        return JcaX509CertificateConverter().getCertificate(certificateHolder)
    }

    suspend fun disconnect(immediately: Boolean) {
        if (sshd == null || !sshd!!.isStarted) return
        val thisNode = fileNodeRepository.getFileNode(this.fileNodeRepository.serviceId) ?: throw IllegalStateException(
            "This file node is not registered in the file cluster!"
        )
        sshd!!.stop(immediately)
        thisNode.endSession()
        thisNode.connected = false
        fileNodeRepository.shutdownAction.run()
        runBlocking { fileNodeRepository.updateFileNode(thisNode) }
        eventManager.fireEvent(FileNodeDisconnectedEvent(thisNode.serviceId))
    }

    suspend fun createSession(serviceId: ServiceId): Session {
        val fileNode = fileNodeRepository.getFileNode(serviceId) ?: throw IllegalStateException(
            "File node with service id $serviceId is not registered in the file cluster!"
        )
        val session = jsch.getSession(fileNode.username, fileNode.hostname, fileNode.port)
        session.setPassword(fileNode.password)
        session.setConfig("serviceId", serviceId.toName())
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect()
        if (!session.isConnected) throw IllegalStateException("Session is not connected!")
        return session
    }

    suspend fun openChannel(session: Session): ChannelSftp {
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect()
        if (!channel.isConnected) throw IllegalStateException("Channel is not connected!")
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
                }catch (_: Exception) {}
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
        return packetManager.publish(UnzipPacket(file, unzipPath), serviceId).withTimeOut(15.seconds).waitBlocking()
    }

    suspend fun requestFile(channel: ChannelSftp, targetFile: String, destinationFile: File): File {
        val serviceId = ServiceId.fromString(
            channel.session.getConfig("serviceId")!!
        )
        val fileNode = fileNodeRepository.getFileNode(serviceId) ?: throw IllegalStateException(
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
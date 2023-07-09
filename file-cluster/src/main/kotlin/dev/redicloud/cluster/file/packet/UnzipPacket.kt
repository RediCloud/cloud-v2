package dev.redicloud.cluster.file.packet

import dev.redicloud.packets.AbstractPacket
import dev.redicloud.utils.toCloudFile
import dev.redicloud.utils.unzipFile
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class UnzipPacket(
    val zipLocation: String,
    val unzipLocation: String
) : AbstractPacket() {

    /*
    Install unzip:
    Windows: https://www.somacon.com/p161.php
    Linux: apt install unzip
     */
    override fun received() {
        val zipLocation = toCloudFile(zipLocation)
        val unzipLocation = toCloudFile(unzipLocation)
        unzipFile(zipLocation.absolutePath, unzipLocation.absolutePath)
        runBlocking { respond(UnzipResponse()) }
    }

}
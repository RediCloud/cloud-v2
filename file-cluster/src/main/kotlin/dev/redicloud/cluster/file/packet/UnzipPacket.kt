package dev.redicloud.cluster.file.packet

import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.utils.toCloudFile
import dev.redicloud.utils.unzipFile
import kotlinx.coroutines.runBlocking

class UnzipPacket(
    val zipLocation: String,
    val unzipLocation: String
) : AbstractPacket() {

    /*
    Install unzip:
    Windows: https://www.somacon.com/p161.php
    Linux: apt install unzip
     */
    override fun received(manager: IPacketManager) {
        super.received(manager)
        val zipLocation = toCloudFile(zipLocation)
        val unzipLocation = toCloudFile(unzipLocation)
        unzipFile(zipLocation.absolutePath, unzipLocation.absolutePath)
        runBlocking { respond(UnzipResponse()) }
    }

}
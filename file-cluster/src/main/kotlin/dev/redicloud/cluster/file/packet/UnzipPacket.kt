package dev.redicloud.cluster.file.packet

import dev.redicloud.packets.AbstractPacket
import dev.redicloud.utils.toCloudFile
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
    override fun received() {
        val zipLocation = toCloudFile(zipLocation)
        val unzipLocation = toCloudFile(unzipLocation)
        val process = Runtime.getRuntime().exec("unzip -o ${zipLocation.absolutePath} -d ${unzipLocation.absolutePath}")
        if (process.isAlive) {
            process.waitFor()
        }
        runBlocking { getManager()!!.publish(UnzipResponse().asAnswerOf(this@UnzipPacket), sender!!) }
    }

}
package dev.redicloud.service.node

import dev.redicloud.utils.prettyPrintGson
import java.io.File
import java.util.UUID

data class NodeConfiguration (
    val nodeName: String,
    val uniqueId: UUID,
    val hostAddress: String
) {

    companion object {
        fun fromFile(file: File): NodeConfiguration {
            return prettyPrintGson.fromJson(file.readText(), NodeConfiguration::class.java)
        }

    }

    fun toFile(file: File) {
        if (file.exists()) file.delete()
        file.createNewFile()
        file.writeText(prettyPrintGson.toJson(this))
    }

}
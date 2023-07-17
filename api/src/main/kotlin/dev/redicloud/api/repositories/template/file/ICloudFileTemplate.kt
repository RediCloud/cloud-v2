package dev.redicloud.api.repositories.template.file

import java.io.File
import java.util.UUID

interface ICloudFileTemplate {

    val uniqueId: UUID
    var prefix: String
    var name: String
    val inherited: MutableList<UUID>
    val displayName: String
    val folder: File
    val prefixFolder: File

}
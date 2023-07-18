package dev.redicloud.api.service.file

import dev.redicloud.api.service.ICloudService

interface IFileNode : ICloudService {

    var port: Int
    var hostname: String
    val nodeInternal: Boolean
    val cloudPath: String

}
package dev.redicloud.api.repositories.service.file

import dev.redicloud.api.repositories.service.ICloudService

interface IFileNode : ICloudService {

    var port: Int
    var hostname: String
    val nodeInternal: Boolean
    val cloudPath: String

}
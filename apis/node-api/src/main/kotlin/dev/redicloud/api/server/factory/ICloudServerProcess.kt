package dev.redicloud.api.server.factory

import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.template.configuration.ICloudConfigurationTemplate
import dev.redicloud.api.utils.ProcessConfiguration
import dev.redicloud.api.utils.ProcessHandler

interface ICloudServerProcess {

    val serverId: ServiceId
    val port: Int
    val configurationTemplate: ICloudConfigurationTemplate
    val processConfiguration: ProcessConfiguration?
    val processHandler: ProcessHandler?
    val hostServiceId: ServiceId
    var process: Process?

}
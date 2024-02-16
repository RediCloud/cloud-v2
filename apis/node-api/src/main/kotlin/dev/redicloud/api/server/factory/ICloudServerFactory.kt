package dev.redicloud.api.server.factory

import dev.redicloud.api.service.server.factory.ICloudRemoteServerFactory

interface ICloudServerFactory : ICloudRemoteServerFactory {

    val hostedProcesses: List<ICloudServerProcess>

}
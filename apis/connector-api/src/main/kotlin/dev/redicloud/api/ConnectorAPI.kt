package dev.redicloud.api

import dev.redicloud.api.provider.IServerPlayerProvider

interface IConnectorAPI {

    var playerProvider: IServerPlayerProvider

}
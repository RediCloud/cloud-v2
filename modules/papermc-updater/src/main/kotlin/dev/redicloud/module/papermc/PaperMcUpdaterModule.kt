package dev.redicloud.module.papermc

import com.google.inject.name.Named
import dev.redicloud.api.modules.ICloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.node.ICloudNodeRepository
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.api.version.ICloudServerVersionRepository
import dev.redicloud.api.version.ICloudServerVersionTypeRepository
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.api.version.IVersionRepository
import dev.redicloud.console.Console
import dev.redicloud.logging.LogManager

class PaperMcUpdaterModule : ICloudModule, CloudInjectable {

    companion object {
        private val logger = LogManager.logger(PaperMcServerVersionHandler::class)
    }

    lateinit var requester: PaperMcApiRequester
    lateinit var handler: PaperMcServerVersionHandler

    @ModuleTask(ModuleLifeCycle.LOAD)
    fun onLoad(
        @Named("this") serviceId: ServiceId,
        serverVersionRepository: ICloudServerVersionRepository,
        serverVersionTypeRepository: ICloudServerVersionTypeRepository,
        javaVersionRepository: ICloudServerVersionRepository,
        nodeRepository: ICloudNodeRepository,
        console: Console,
        versionRepository: IVersionRepository
    ) {
        requester = PaperMcApiRequester(versionRepository)
        handler = PaperMcServerVersionHandler(
            serviceId,
            serverVersionRepository,
            serverVersionTypeRepository,
            javaVersionRepository,
            nodeRepository,
            console,
            versionRepository,
            requester,
            logger
        )
        IServerVersionHandler.registerHandler(handler)
    }

    @ModuleTask(ModuleLifeCycle.UNLOAD)
    fun onUnload() {
        IServerVersionHandler.unregisterHandler(handler)
    }

}
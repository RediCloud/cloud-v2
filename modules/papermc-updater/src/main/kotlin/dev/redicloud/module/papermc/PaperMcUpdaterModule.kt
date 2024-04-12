package dev.redicloud.module.papermc

import com.google.inject.name.Named
import dev.redicloud.api.modules.CloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.api.version.ICloudServerVersionRepository
import dev.redicloud.api.version.ICloudServerVersionTypeRepository
import dev.redicloud.api.version.IServerVersionHandler
import dev.redicloud.api.version.IVersionRepository
import dev.redicloud.console.Console
import dev.redicloud.logging.LogManager

class PaperMcUpdaterModule : CloudModule(), CloudInjectable {

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
        versionRepository: IVersionRepository,
        console: Console
    ) {
        requester = PaperMcApiRequester(versionRepository)
        handler = PaperMcServerVersionHandler(
            serverVersionRepository,
            serverVersionTypeRepository,
            requester,
            console,
            logger
        )
        IServerVersionHandler.registerHandler(handler)
    }

    @ModuleTask(ModuleLifeCycle.UNLOAD)
    fun onUnload() {
        IServerVersionHandler.unregisterHandler(handler)
    }

}
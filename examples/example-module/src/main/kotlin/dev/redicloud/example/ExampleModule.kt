package dev.redicloud.example

import com.google.inject.Inject
import com.google.inject.name.Named
import dev.redicloud.api.modules.CloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.api.modules.ModuleTaskOrder
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.utils.CloudInjectable
import java.util.logging.Logger

class ExampleModule @Inject constructor(
    @Named("service") val logger: Logger
) : CloudModule(), CloudInjectable {

    val config = getStorage("config")

    @ModuleTask(ModuleLifeCycle.LOAD, ModuleTaskOrder.EARLY)
    suspend fun load(@Named("this") serviceId: ServiceId) {
        if (config.getOrDefault("first-load", { true }, Boolean::class.java)) {
            logger.info("First load task: ${serviceId.toName()}")
            config.set("first-load", false)
        }
    }

    @ModuleTask(ModuleLifeCycle.UNLOAD, ModuleTaskOrder.FIRST)
    fun firstUnload(@Named("this") serviceId: ServiceId) {
        logger.info("First unload task: ${serviceId.toName()}")
    }

    @ModuleTask(ModuleLifeCycle.UNLOAD, ModuleTaskOrder.LAST)
    fun earlyUnload(@Named("this") serviceId: ServiceId) {
        logger.info("Last unload task: ${serviceId.toName()}")
    }

}
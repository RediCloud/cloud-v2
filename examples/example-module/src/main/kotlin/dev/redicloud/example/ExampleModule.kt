package dev.redicloud.example

import com.google.inject.name.Named
import dev.redicloud.api.modules.ICloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.api.modules.ModuleTaskOrder
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.utils.CloudInjectable
import java.util.logging.LogManager

class ExampleModule : ICloudModule, CloudInjectable {

    companion object {
        private val logger = LogManager.getLogManager().getLogger(ExampleModule::class.java.name)
    }

    @ModuleTask(ModuleLifeCycle.LOAD, ModuleTaskOrder.FIRST)
    fun first(@Named("this") serviceId: ServiceId) {
        logger.info("First task: ${serviceId.toName()}")
    }

    @ModuleTask(ModuleLifeCycle.LOAD, ModuleTaskOrder.EARLY)
    fun early(@Named("this") serviceId: ServiceId) {
        logger.info("Early task: ${serviceId.toName()}")
    }

    @ModuleTask(ModuleLifeCycle.LOAD, ModuleTaskOrder.DEFAULT)
    fun default(@Named("this") serviceId: ServiceId) {
        logger.info("Default task: ${serviceId.toName()}")
    }

    @ModuleTask(ModuleLifeCycle.LOAD, ModuleTaskOrder.LATE)
    fun late(@Named("this") serviceId: ServiceId) {
        logger.info("Late task: ${serviceId.toName()}")
    }

    @ModuleTask(ModuleLifeCycle.LOAD, ModuleTaskOrder.LAST)
    fun last(@Named("this") serviceId: ServiceId) {
        logger.info("Last task: ${serviceId.toName()}")
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
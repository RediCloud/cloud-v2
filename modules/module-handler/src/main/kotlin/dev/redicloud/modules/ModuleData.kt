package dev.redicloud.modules

import dev.redicloud.api.modules.ICloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import java.io.File

data class ModuleData(
    val id: String,
    val file: File,
    val description: ModuleDescription,
    var lifeCycle: ModuleLifeCycle,
    var loaded: Boolean
) {

    lateinit var instance: ICloudModule

    fun init(instance: ICloudModule) {
        this.instance = instance
    }

}
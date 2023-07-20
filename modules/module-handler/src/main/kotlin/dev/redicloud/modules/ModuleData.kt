package dev.redicloud.modules

import dev.redicloud.api.modules.ICloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import java.io.File

data class ModuleData<T : ICloudModule>(
    val id: String,
    val module: T,
    val file: File,
    val description: ModuleDescription,
    var lifeCycle: ModuleLifeCycle,
    var loader: ModuleClassLoader,
    val tasks: List<ModuleTaskData>,
    var loaded: Boolean
)
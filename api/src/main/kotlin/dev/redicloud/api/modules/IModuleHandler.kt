package dev.redicloud.api.modules

interface IModuleHandler {

    suspend fun updateModules(silent: Boolean = false, loadModules: Boolean = false)

    fun getState(moduleId: String): ModuleLifeCycle?

    fun getState(module: ICloudModule): ModuleLifeCycle?

    fun <T : ICloudModule> getModule(module: String): T?

    fun reload(module: ICloudModule)

    fun reload(moduleId: String)

    fun unload(module: ICloudModule)

    fun unload(moduleId: String)

    fun load(module: ICloudModule)

    fun load(moduleId: String)

}
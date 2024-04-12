package dev.redicloud.api.modules

interface IModuleHandler {

    suspend fun updateModules(silent: Boolean = false, loadModules: Boolean = false)

    fun getState(moduleId: String): ModuleLifeCycle?

    fun isModuleReloadable(moduleId: String): Boolean

    fun reloadModule(moduleId: String)

    fun unloadModule(moduleId: String)

    fun loadModule(moduleId: String)

    fun getStorage(moduleId: String, name: String): IModuleStorage

    fun getDescription(moduleId: String): IModuleDescription

}
package dev.redicloud.api.modules

interface IModuleHandler {

    suspend fun updateModules(silent: Boolean = false, loadModules: Boolean = false)

    fun getState(moduleId: String): ModuleLifeCycle?

    fun isModuleReloadable(moduleId: String): Boolean

    suspend fun reloadModule(moduleId: String)

    suspend fun unloadModule(moduleId: String)

    suspend fun loadModule(moduleId: String)

    fun getStorage(moduleId: String, name: String): IModuleStorage

    fun getDescription(moduleId: String): IModuleDescription
}

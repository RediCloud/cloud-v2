package dev.redicloud.api.modules

abstract class CloudModule {
    private lateinit var moduleId: String
    private lateinit var moduleHandler: IModuleHandler

    fun getStorage(name: String): IModuleStorage {
        return moduleHandler.getStorage(moduleId, name)
    }
}
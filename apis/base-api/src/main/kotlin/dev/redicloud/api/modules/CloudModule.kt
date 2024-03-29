package dev.redicloud.api.modules

abstract class CloudModule {
    private lateinit var moduleId: String
    private val moduleHandler: IModuleHandler? = null

    fun getStorage(name: String): IModuleStorage {
        return moduleHandler!!.getStorage(moduleId, name)
    }
}
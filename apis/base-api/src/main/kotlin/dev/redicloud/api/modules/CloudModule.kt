package dev.redicloud.api.modules

abstract class CloudModule {

    private lateinit var moduleId: String
    private val moduleHandler: IModuleHandler? = null

    val description: IModuleDescription
        get() = moduleHandler!!.getDescription(moduleId)
    val state: ModuleLifeCycle
        get() = moduleHandler!!.getState(moduleId)!!


    fun getStorage(name: String): IModuleStorage {
        return moduleHandler!!.getStorage(moduleId, name)
    }

}
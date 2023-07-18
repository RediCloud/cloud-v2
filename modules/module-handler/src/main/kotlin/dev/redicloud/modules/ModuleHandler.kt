package dev.redicloud.modules

import dev.redicloud.api.modules.ICloudModule
import dev.redicloud.api.modules.IModuleHandler
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.api.utils.MODULE_FOLDER
import dev.redicloud.api.utils.injector
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.apply.impl.JarResourceLoader
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.gson.gson
import jdk.internal.misc.VM
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import java.util.jar.JarFile
import kotlin.concurrent.withLock
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

class ModuleHandler(
    private val serviceId: ServiceId
) : IModuleHandler {

    companion object {
        private val logger = LogManager.logger(ModuleHandler::class)
    }

    private val modules = mutableListOf<ModuleData<*>>()
    private val lock = ReentrantLock()
    private val moduleFiles = mutableListOf<File>()

    fun detectModules() = lock.withLock {
        moduleFiles.clear()
        MODULE_FOLDER.getFile().listFiles()?.filter {
            it.isFile && it.extension == "jar"
        }?.filter {
            val jarFile = JarFile(it)
            val moduleInfo = jarFile.getJarEntry("module.json")
            if (moduleInfo == null) {
                logger.warning("Found jar file without module.json: ${it.name}")
                return@filter false
            }else true
        }?.forEach {
            moduleFiles.add(it)
        }
    }

    fun loadModule(file: File) = lock.withLock {
        if (modules.any { it.file == file }) {
            logger.warning("Tried to load module that is already loaded: ${file.name}")
            return
        }
        if (!moduleFiles.contains(file)) {
            logger.warning("Tried to load module that is not detected as module: ${file.name}")
            return
        }

        val jarFile = JarFile(file)
        val moduleInfoEntry = jarFile.getJarEntry("module.json")!!
        val inputStream = jarFile.getInputStream(moduleInfoEntry)
        val description = gson.fromJson(inputStream.reader().readText(), ModuleDescription::class.java)

        if (modules.any { it.id == description.id }) {
            logger.warning("Tried to load module with id that is already loaded: ${description.id}")
            return
        }

        if (!description.mainClasses.containsKey(serviceId.type)) return@withLock

        val loader = ModuleClassLoader(description.id, arrayOf(file.toURI().toURL()), this.javaClass.classLoader)

        try {
            Bootstrap().apply(loader, loader, JarResourceLoader(description.id, file))
        }catch (_: Exception) {
            // No library loader found, can be ignored
        }

        val moduleClass = loader.loadClass(description.mainClasses[serviceId.type]!!).kotlin
        if (!moduleClass.isSubclassOf(ICloudModule::class)) {
            logger.warning("Main class of module ${description.id} is not a subclass of ICloudModule!")
            return@withLock
        }
        var moduleInstance: ICloudModule? = null
        try {
            moduleInstance = if (moduleClass.isSubclassOf(CloudInjectable::class)) {
                injector.getInstance(moduleClass.java)
            }else {
                moduleClass.createInstance()
            } as ICloudModule
        }catch (e: Exception) {
            logger.warning("Failed to load module ${description.id}!", e)
            return@withLock
        }

        val moduleData = ModuleData(description.id, moduleInstance, file, description, ModuleLifeCycle.UNLOAD, loader)
        modules.add(moduleData)

        this.runModuleTasks(moduleInstance, ModuleLifeCycle.LOAD)
    }

    fun runModuleTasks(module: ICloudModule, lifeCycle: ModuleLifeCycle) {
        TODO("Not yet implemented")
    }

    override fun getState(moduleId: String): ModuleLifeCycle? {
        return modules.firstOrNull { it.id == moduleId }?.lifeCycle
    }

    override fun getState(module: ICloudModule): ModuleLifeCycle? {
        return modules.firstOrNull { it.module == module }?.lifeCycle
    }

    override fun <T : ICloudModule> getModule(module: String): T? {
        return modules.firstOrNull { it.id == module }?.module as? T
    }

    override fun reload(module: ICloudModule) {
        TODO("Not yet implemented")
    }

    override fun reload(moduleId: String) {
        TODO("Not yet implemented")
    }

    override fun unload(module: ICloudModule) {
        TODO("Not yet implemented")
    }

    override fun unload(moduleId: String) {
        TODO("Not yet implemented")
    }

    override fun load(module: ICloudModule) {
        TODO("Not yet implemented")
    }

    override fun load(moduleId: String) {
        TODO("Not yet implemented")
    }

}
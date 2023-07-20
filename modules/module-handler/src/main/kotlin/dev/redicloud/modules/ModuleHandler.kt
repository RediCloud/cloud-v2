package dev.redicloud.modules

import com.google.inject.Key
import com.google.inject.name.Named
import com.google.inject.name.Names
import dev.redicloud.api.modules.ICloudModule
import dev.redicloud.api.modules.IModuleHandler
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.api.utils.MODULE_FOLDER
import dev.redicloud.api.utils.injector
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.apply.impl.JarResourceLoader
import dev.redicloud.logging.LogManager
import dev.redicloud.utils.gson.gson
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import java.util.jar.JarFile
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.reflect

class ModuleHandler(
    private val serviceId: ServiceId
) : IModuleHandler {

    companion object {
        private val logger = LogManager.logger(ModuleHandler::class)
    }

    private val modules = mutableListOf<ModuleData<*>>()
    private val lock = ReentrantLock()
    private val moduleFiles = mutableListOf<File>()
    private val cachedDescription = mutableListOf<ModuleDescription>()

    fun loadModules() {
        detectModules()
        moduleFiles.forEach {
            loadModule(it)
        }
    }

    fun unloadModules() {
        modules.forEach {
            unloadModule(it)
        }
    }

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
            loadDescription(it)
        }
    }

    fun loadDescription(file: File): ModuleDescription {
        val jarFile = JarFile(file)
        val moduleInfoEntry = jarFile.getJarEntry("module.json")!!
        val inputStream = jarFile.getInputStream(moduleInfoEntry)
        val description = gson.fromJson(inputStream.reader().readText(), ModuleDescription::class.java)
        description.cachedFile = file
        cachedDescription.removeIf { it.id == description.id }
        cachedDescription.add(description)
        return description
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

        val description = loadDescription(file)

        if (modules.any { it.id == description.id && it.loaded}) {
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

        val tasks = mutableListOf<ModuleTaskData>()
        (moduleClass.declaredMemberFunctions + moduleClass.declaredMemberExtensionFunctions).filter {
            it.hasAnnotation<ModuleTask>()
        }.forEach {
            val annotation = it.findAnnotation<ModuleTask>()!!
            tasks.add(ModuleTaskData(it, annotation.lifeCycle, annotation.order))
        }

        val moduleData = ModuleData(description.id, moduleInstance, file, description, ModuleLifeCycle.UNLOAD, loader, tasks, false)

        if (tasks.isEmpty()) {
            logger.warning("Module ${description.id} has no tasks!")
        }

        try {
            val tasksCount = callTasks(moduleData, ModuleLifeCycle.LOAD)
            modules.add(moduleData)
            logger.info("Loaded module ${description.id} with $tasksCount load tasks!")
            moduleData.loaded = true
        }catch (e: Exception) {
            moduleData.lifeCycle = ModuleLifeCycle.UNLOAD
            logger.warning("Failed to load module ${description.id}!", e)
            return@withLock
        }
    }

    fun reloadModule(moduleData: ModuleData<*>) = lock.withLock {
        if (moduleData.lifeCycle != ModuleLifeCycle.LOAD) {
            logger.warning("Tried to reload module ${moduleData.id} that is not loaded!")
            return
        }
        try {
            val tasksCount = callTasks(moduleData, ModuleLifeCycle.RELOAD)
            moduleData.lifeCycle = ModuleLifeCycle.LOAD
            logger.info("Reloaded module ${moduleData.id} with $tasksCount reload tasks!")
        }catch (e: Exception) {
            moduleData.lifeCycle = ModuleLifeCycle.UNLOAD
            logger.warning("Failed to reload module ${moduleData.id}!", e)
            return@withLock
        }
    }

    fun unloadModule(moduleData: ModuleData<*>) = lock.withLock {
        if (moduleData.lifeCycle != ModuleLifeCycle.LOAD || modules.none { it.id == moduleData.id && it.loaded }) {
            logger.warning("Tried to unload module ${moduleData.id} that is not loaded!")
            return
        }
        try {
            moduleData.loaded = false
            val tasksCount = callTasks(moduleData, ModuleLifeCycle.UNLOAD)
            logger.info("Unloaded module ${moduleData.id} with $tasksCount unload tasks!")
        }catch (e: Exception) {
            moduleData.lifeCycle = ModuleLifeCycle.UNLOAD
            logger.warning("Failed to unload module ${moduleData.id}!", e)
            return@withLock
        }
    }

    internal fun callTasks(moduleData: ModuleData<*>, targetLifeCycle: ModuleLifeCycle): Int {
        val tasks = moduleData.tasks
        var tasksCount = 0
        moduleData.lifeCycle = targetLifeCycle
        tasks.filter { it.lifeCycle == targetLifeCycle }.sortedBy { it.order }.forEach {
            val function = it.function
            val injectParameters = mutableListOf<Any>()
            function.javaMethod!!.parameters.forEach {
                val type = it.type.kotlin
                val key = if (it.isAnnotationPresent(Named::class.java)) {
                    Key.get(type.java, it.getAnnotation(Named::class.java))
                }else Key.get(type.java)
                val instance = injector.getInstance(key)
                injectParameters.add(instance)
            }
            if (function.isSuspend) { //TODO test suspend
                runBlocking { function.callSuspend(moduleData.module, *injectParameters.toTypedArray()) }
            }else {
                function.javaMethod!!.invoke(moduleData.module, *injectParameters.toTypedArray())
            }
            tasksCount++
        }
        return tasksCount
    }

    fun getModuleDescription(moduleId: String): ModuleDescription? {
        return cachedDescription.firstOrNull { it.id == moduleId }
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
        reloadModule(modules.firstOrNull { it.module == module } ?: return)
    }

    override fun reload(moduleId: String) {
        reloadModule(modules.firstOrNull { it.id == moduleId } ?: return)
    }

    override fun unload(module: ICloudModule) {
        unloadModule(modules.firstOrNull { it.module == module } ?: return)
    }

    override fun unload(moduleId: String) {
        unloadModule(modules.firstOrNull { it.id == moduleId } ?: return)
    }

    override fun load(module: ICloudModule) {
        loadModule(modules.firstOrNull { it.module == module }?.file ?: return)
    }

    override fun load(moduleId: String) {
        loadModule(modules.firstOrNull { it.id == moduleId }?.file ?: return)
    }

    fun getModuleDatas(): List<ModuleData<*>> {
        return modules
    }

}
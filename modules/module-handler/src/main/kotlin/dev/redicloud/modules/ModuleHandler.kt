package dev.redicloud.modules

import com.google.inject.Key
import com.google.inject.name.Named
import dev.redicloud.api.modules.*
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.utils.CloudInjectable
import dev.redicloud.api.utils.MODULE_FOLDER
import dev.redicloud.api.utils.injector
import dev.redicloud.api.version.ICloudServerVersionType
import dev.redicloud.commands.api.SUGGESTERS
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.event.EventManager
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.apply.impl.JarResourceLoader
import dev.redicloud.logging.LogManager
import dev.redicloud.modules.repository.ModuleWebRepository
import dev.redicloud.modules.suggesters.*
import dev.redicloud.packets.PacketManager
import dev.redicloud.utils.gson.gson
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import java.util.jar.JarFile
import kotlin.concurrent.withLock
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod

class ModuleHandler(
    private val serviceId: ServiceId,
    repoUrls: List<String>,
    private val eventManager: EventManager,
    private val packetManager: PacketManager,
    private val serverVersionType: ICloudServerVersionType?,
    private val databaseConnection: DatabaseConnection
) : IModuleHandler {

    companion object {
        private val logger = LogManager.logger(ModuleHandler::class)
    }

    private val loaders = mutableMapOf<String, ModuleClassLoader>()
    private val lock = ReentrantLock()
    private val moduleFiles = mutableListOf<File>()
    private val cachedDescriptions = mutableListOf<ModuleDescription>()
    val repositories = mutableListOf<ModuleWebRepository>()
    private val storages = mutableListOf<ModuleStorage>()

    init {
        repoUrls.forEach { url ->
            try {
                ModuleWebRepository(url, this).also {
                    repositories.add(it)
                }
            }catch (e: Exception) {
                logger.severe("Failed to add module repository $url!", e)
            }
        }
        SUGGESTERS.add(InstallableModulesSuggester(this))
        SUGGESTERS.add(LoadableModulesSuggester(this))
        SUGGESTERS.add(ReloadableModulesSuggester(this))
        SUGGESTERS.add(UninstallableModulesSuggester(this))
        SUGGESTERS.add(UnloadableModulesSuggester(this))
        SUGGESTERS.add(InstalledModulesSuggester(this))
    }

    suspend fun loadModules() {
        detectModules()
        moduleFiles.forEach {
            loadModule(it)
        }
    }

    override suspend fun updateModules(silent: Boolean, loadModules: Boolean) = lock.withLock {
        runBlocking {
            cachedDescriptions.forEach { description ->
                try {
                    val targetRepositories = if (description.cachedFile != null) {
                        repositories.filter { it.isUpdateAvailable(description.id) }
                    }else emptyList()

                    if (targetRepositories.isEmpty()) return@forEach

                    if (targetRepositories.size > 2) {
                        logger.warning("§cFound more than 2 repositories that have an update for module ${description.id}!")
                        targetRepositories.forEach { logger.warning("§c - ${it.repoUrl} | ${it.getLatestVersion(description.id)}") }
                        return@forEach
                    }

                    val targetRepository = targetRepositories.first()
                    val data = getModuleData(description.id)
                    if (data != null && (data.loaded || data.lifeCycle == ModuleLifeCycle.LOAD)) {
                        unloadModule(data.id)
                    }
                    targetRepository.download(description.id, description.version)
                    detectModules()
                    val file = moduleFiles.firstOrNull { it.name == "${description.id}-${description.version}.jar" }
                    if (file == null) {
                        logger.warning("§cFailed to find downloaded module ${description.id}!")
                        return@forEach
                    }
                    if (loadModules) loadModule(file)
                }catch (e: Exception) {
                    logger.severe("Failed to update module ${description.id}!", e)
                }
            }
        }
    }

    suspend fun uninstall(moduleId: String) {
        logger.info("Uninstalling module %hc%$moduleId%tc%...")
        val file = getModuleData(moduleId).also { data ->
            if (data == null) return@also
            if (data.loaded) unloadModule(data.id)
            moduleFiles.removeIf { it.name == data.description.name }
        }?.file ?: cachedDescriptions.firstOrNull { it.id == moduleId }?.cachedFile
        if (file == null) {
            logger.info("§cModule with id $moduleId not found!")
            return
        }
        if (file.delete()) {
            cachedDescriptions.removeIf { it.id == moduleId }
            loaders.remove(moduleId)?.close()
            logger.info("Module with id %hc%$moduleId %tc%uninstalled!")
        } else {
            file.deleteOnExit()
            logger.warning("§cFailed to uninstall module with id $moduleId instantly!")
            logger.warning("§cThe module file will be deleted on the next node stop!")
        }
    }

    suspend fun install(moduleId: String, load: Boolean = true) {
        if (getModuleDescription(moduleId) != null) {
            logger.info("§cModule with id $moduleId already installed!")
            return
        }
        val repository = repositories.firstOrNull { it.hasModule(moduleId) }
        if (repository == null) {
            logger.warning("§cModule with id $moduleId not found!")
            return
        }
        val info = repository.getModuleInfo(moduleId)
        if (info == null) {
            logger.warning("§cModule with id $moduleId not found!")
            return
        }
        val latest = repository.getLatestVersion(info.id)
        if (latest == null) {
            logger.warning("§cModule with id $moduleId not found!")
            return
        }
        logger.info("Installing module %hc%${info.id} §8(%tc%${latest}§8)%tc%...")
        try {
            val file = repository.download(info.id, latest)
            if (file.length() < 1000) {
                logger.warning("§cFailed to install module with id $moduleId!")
                return
            }
            logger.info("Module with id %hc%$moduleId%tc% installed!")
            if (load) {
                loadModule(file)
            }else {
                logger.info("Use 'module load $moduleId' to load the module!")
            }
        }catch (e: Exception) {
            logger.warning("§cFailed to install module with id $moduleId!")
            return
        }
    }

    fun unloadModules() {
        loaders.map { it.value.data.id }.toList().forEach {
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
        cachedDescriptions.removeIf { it.id == description.id }
        cachedDescriptions.add(description)
        return description
    }

    fun loadModule(file: File) = lock.withLock {
        if (!file.exists()) {
            logger.warning("§cTried to load module that does not exist: ${file.name}")
            return
        }
        if (file.extension != "jar") {
            logger.warning("§cTried to load module that is not a jar file: ${file.name}")
            return
        }
        if (loaders.any { it.value.data.file == file }) {
            logger.warning("§cTried to load module that is already loaded: ${file.name}")
            return
        }

        val description = loadDescription(file)

        if (loaders.any { it.value.data.id == description.id }) {
            logger.warning("§cTried to load module that is already loaded: ${description.id}")
            return
        }

        val identifierTypes = mutableListOf<String>()
        identifierTypes.add(serviceId.type.name.lowercase())
        if (serverVersionType != null) identifierTypes.add("${serviceId.type.name}_${serverVersionType.name}".lowercase())

        val matchedMain: String = description.mainClasses
            .filter { identifierTypes.contains(it.key.lowercase()) }
            .values.firstOrNull() ?: return@withLock

        val moduleData = ModuleData(description.id, file, description, ModuleLifeCycle.UNLOAD, false)
        val loader = ModuleClassLoader(moduleData, file, this.javaClass.classLoader)

        try {
            Bootstrap().apply(loader, loader, JarResourceLoader(description.id, file))
        }catch (_: Exception) {
            // No library loader found, can be ignored
        }

        val moduleClass = loader.loadClass(matchedMain).kotlin
        if (!moduleClass.isSubclassOf(ICloudModule::class)) {
            logger.warning("§cMain class of module ${description.id} is not a subclass of ICloudModule!")
            return@withLock
        }
        loaders[description.id] = loader
        val moduleInstance: ICloudModule?
        try {
            moduleInstance = if (moduleClass.isSubclassOf(CloudInjectable::class)) {
                injector.getInstance(moduleClass.java)
            }else {
                moduleClass.createInstance()
            } as ICloudModule
        }catch (e: Exception) {
            logger.warning("§cFailed to load module ${description.id}!", e)
            return@withLock
        }
        moduleData.init(moduleInstance)

        val tasks = mutableListOf<ModuleTaskData>()
        (moduleClass.declaredMemberFunctions + moduleClass.declaredMemberExtensionFunctions).filter {
            it.hasAnnotation<ModuleTask>()
        }.forEach {
            val annotation = it.findAnnotation<ModuleTask>()!!
            tasks.add(ModuleTaskData(it, annotation.lifeCycle, annotation.order))
        }

        loader.init(tasks)

        if (tasks.isEmpty()) {
            logger.warning("§cModule ${description.id} has no tasks!")
        }

        try {
            val tasksCount = callTasks(moduleData.id, ModuleLifeCycle.LOAD)
            logger.info("Loaded module %hc%${description.id}%tc% with %hc%$tasksCount%tc% load tasks!")
            moduleData.loaded = true
        }catch (e: Exception) {
            moduleData.lifeCycle = ModuleLifeCycle.UNLOAD
            logger.warning("§cFailed to load module ${description.id}!", e)
            return@withLock
        }
    }

    override fun reloadModule(moduleId: String) = lock.withLock {
        val moduleData = getModuleData(moduleId)
        if (moduleData == null) {
            logger.warning("§cTried to reload module $moduleId that is not loaded!")
            return
        }
        if (moduleData.lifeCycle != ModuleLifeCycle.LOAD) {
            logger.warning("§cTried to reload module ${moduleData.id} that is not loaded!")
            return
        }
        if (!isModuleReloadable(moduleId)) {
            logger.warning("§cTried to reload module ${moduleData.id} that is not reloadable!")
            return
        }
        try {
            val tasksCount = callTasks(moduleData.id, ModuleLifeCycle.RELOAD)
            moduleData.lifeCycle = ModuleLifeCycle.LOAD
            logger.info("Reloaded module %hc%${moduleData.id}%tc% with %hc%$tasksCount%tc% reload tasks!")
        }catch (e: Exception) {
            moduleData.lifeCycle = ModuleLifeCycle.UNLOAD
            logger.warning("§cFailed to reload module ${moduleData.id}!", e)
            return@withLock
        }
    }

    override fun unloadModule(moduleId: String) = lock.withLock {
        if (!loaders.containsKey(moduleId)) {
            logger.warning("§cTried to unload module $moduleId that is not loaded!")
            return
        }
        try {
            val file = getModuleData(moduleId)!!.file
            getModuleData(moduleId)!!.loaded = false
            val tasksCount = callTasks(moduleId, ModuleLifeCycle.UNLOAD)
            loaders.remove(moduleId)?.also {
                packetManager.unregister(it)
                eventManager.unregister(it)
                it.close()
            }
            JarFile(file).close()
            logger.info("Unloaded module %hc%$moduleId%tc% with %hc%$tasksCount%tc% unload tasks!")
        }catch (e: Exception) {
            logger.warning("§cFailed to unload module $moduleId!", e)
            return@withLock
        }
    }

    suspend fun getRepository(moduleId: String): ModuleWebRepository? {
        return repositories.firstOrNull { it.hasModule(moduleId) }
    }

    internal fun callTasks(moduleId: String, targetLifeCycle: ModuleLifeCycle): Int {
        val moduleData = getModuleData(moduleId) ?: return 0
        val loader = loaders[moduleData.id] ?: return 0
        var tasksCount = 0
        moduleData.lifeCycle = targetLifeCycle
        loader.tasks.filter { it.lifeCycle == targetLifeCycle }.sortedBy { it.order }.forEach {
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
                runBlocking { function.callSuspend(moduleData.instance, *injectParameters.toTypedArray()) }
            }else {
                function.javaMethod!!.invoke(moduleData.instance, *injectParameters.toTypedArray())
            }
            tasksCount++
        }
        return tasksCount
    }

    fun getModuleDescription(moduleId: String): ModuleDescription? {
        return cachedDescriptions.firstOrNull { it.id == moduleId }
    }

    fun getModuleData(moduleId: String): ModuleData? {
        return loaders[moduleId]?.data
    }

    override fun getState(moduleId: String): ModuleLifeCycle? {
        return getModuleData(moduleId)?.lifeCycle
    }

    override fun isModuleReloadable(moduleId: String): Boolean {
        return loaders[moduleId]?.tasks?.filter { it.lifeCycle == ModuleLifeCycle.RELOAD }?.isNotEmpty() ?: false
    }

    override fun loadModule(moduleId: String) {
        loadModule(cachedDescriptions.firstOrNull { it.id == moduleId }?.cachedFile ?: return)
    }

    override fun getStorage(moduleId: String, name: String): ModuleStorage {
        return storages.firstOrNull { it.moduleId == moduleId } ?: ModuleStorage(moduleId, name, databaseConnection)
    }

    fun getModuleDatas(): List<ModuleData> {
        return loaders.map { it.value.data }
    }

    fun getCachedDescriptions(): List<ModuleDescription> {
        return cachedDescriptions
    }

}
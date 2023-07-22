package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.modules.ModuleHandler
import dev.redicloud.modules.repository.ModuleWebRepository
import dev.redicloud.modules.suggesters.*
import dev.redicloud.service.base.utils.ClusterConfiguration
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.mapTo
import dev.redicloud.utils.toSymbol
import kotlinx.coroutines.launch

@Command("module")
@CommandAlias(["modules"])
@CommandDescription("Manage all modules")
class ModuleCommand(
    private val moduleHandler: ModuleHandler,
    private val clusterConfiguration: ClusterConfiguration
) : ICommand {

    @CommandSubPath("list")
    @CommandDescription("List all modules")
    fun list(actor: ConsoleActor) = defaultScope.launch {
        val modules = moduleHandler.getModuleDatas()
        actor.sendHeader("Modules")
        actor.sendMessage("")
        val loaded = modules.filter { it.lifeCycle == ModuleLifeCycle.LOAD }.map { it.description }
        actor.sendMessage("Loaded §8(%hc%${loaded.size}§8)")
        loaded.forEach {
            actor.sendMessage("§8- %hc%${it.id}%tc% §8(%tc%${it.version}§8)")
            actor.sendMessage("\t§8➥ %tc%${it.description}")
        }
        actor.sendMessage("")
        val unloaded = modules
            .filter { it.lifeCycle == ModuleLifeCycle.UNLOAD }
            .map { it.description } + moduleHandler.getCachedDescriptions()
            .filter { desc -> loaded.none { it.id == desc.id } }
        actor.sendMessage("Unloaded §8(%hc%${unloaded.size}§8)")
        unloaded.forEach {
            actor.sendMessage("§8- %hc%${it.id}%tc% §8(%tc%${it.version}§8)")
            actor.sendMessage("\t§8➥ %tc%${it.description}")
        }
        val moduleInfos = moduleHandler.repositories
            .flatMap { repo -> repo.getModuleIds().mapNotNull { repo.getModuleInfo(it) } }
            .filter { desc -> loaded.none { it.id == desc.id } }
            .filter { desc -> unloaded.none { it.id == desc.id } }
        actor.sendMessage("")
        actor.sendMessage("Available §8(%hc%${moduleInfos.size}§8)")
        moduleInfos.forEach {
            val repo = moduleHandler.getRepository(it.id)!!
            actor.sendMessage("§8- %hc%${it.id}%tc% §8(%tc%${it.versions.lastOrNull() ?: "unknown"}§8)")
            actor.sendMessage("\t§8➥ %tc%${it.description}")
            actor.sendMessage("\t§8➥ %tc%${repo.repoUrl}")
        }
        actor.sendMessage("")
        actor.sendHeader("Modules")
    }

    @CommandSubPath("load <id>")
    @CommandDescription("Load a module")
    fun load(
        actor: ConsoleActor,
        @CommandParameter("id", true, LoadableModulesSuggester::class) id: String
    ) {
        moduleHandler.detectModules()
        val description = moduleHandler.getModuleDescription(id)
        if (description == null) {
            actor.sendMessage("§cModule with id $id not found!")
            return
        }
        val data = moduleHandler.getModuleDatas().firstOrNull { it.description.id == id }
        if (data != null && data.loaded) {
            actor.sendMessage("§cModule with id $id is already loaded!")
            return
        }
        actor.sendMessage("Loading module %hc%${description.id}%tc%...")
        moduleHandler.loadModule(description.cachedFile!!)
    }

    @CommandSubPath("unload <id>")
    @CommandDescription("Unload a module")
    fun unload(
        @CommandParameter("id", true, UnloadableModulesSuggester::class) id: String
    ) {
        moduleHandler.unloadModule(id)
    }

    @CommandSubPath("reload <id>")
    @CommandDescription("Reload a module")
    fun reload(
        actor: ConsoleActor,
        @CommandParameter("id", true, ReloadableModulesSuggester::class) id: String
    ) {
        moduleHandler.reloadModule(id)
    }

    @CommandSubPath("info <id>")
    @CommandDescription("Get info about a module")
    fun info(
        actor: ConsoleActor,
        @CommandParameter("id", true, InstalledModulesSuggester::class) id: String
    ) = defaultScope.launch {
        moduleHandler.detectModules()
        val data = moduleHandler.getModuleDatas().firstOrNull { it.description.id == id }
        if (data == null) {
            actor.sendMessage("§cModule with id $id not found!")
            return@launch
        }
        val targetRepository = moduleHandler.getRepository(data.id)
        actor.sendHeader("Module Info")
        actor.sendMessage("Name§8: %hc%${data.description.name}")
        actor.sendMessage("ID§8: %hc%${data.description.id}")
        actor.sendMessage("Repository§8: %hc%${targetRepository?.repoUrl ?: "None"}")
        actor.sendMessage("Update available§8: %hc%${(targetRepository?.isUpdateAvailable(data.id) ?: false).toSymbol()}")
        actor.sendMessage("Loaded§8: %hc%${data.loaded.toSymbol()}")
        actor.sendMessage("Version§8: %hc%${data.description.version}")
        actor.sendMessage("Description§8: %hc%${data.description.description}")
        actor.sendMessage("Website§8: %hc%${data.description.website}")
        actor.sendMessage("Authors§8: %hc%${data.description.authors.joinToString("§8, %hc%")}")
        actor.sendMessage("Supported services§8: %hc%${data.description.mainClasses.keys.joinToString("§8, %hc%") { it.name }}")
        actor.sendHeader("Module Info")
    }

    @CommandSubPath("repository list")
    @CommandDescription("List all repositories")
    fun repositoriesList(
        actor: ConsoleActor
    ) {
        val repositoryUrls = clusterConfiguration.getList<String>("module-repositories")
        if (repositoryUrls.isEmpty()) {
            actor.sendMessage("§cNo repositories found!")
            return
        }
        actor.sendHeader("Repositories")
        actor.sendMessage("")
        repositoryUrls.forEach {
            actor.sendMessage("§8- %hc%$it")
        }
        actor.sendMessage("")
        actor.sendHeader("Repositories")
    }

    @CommandSubPath("repository add <url>")
    @CommandDescription("Add a repository")
    fun repositoriesAdd(
        actor: ConsoleActor,
        @CommandParameter("url") url: String
    ) {
        val repositoryUrls = clusterConfiguration.getList<String>("module-repositories").toMutableList()
        if (repositoryUrls.any { it.lowercase() == url.lowercase() }) {
            actor.sendMessage("§cRepository with url $url already exists!")
            return
        }
        try {
            val repo = ModuleWebRepository(url, this.moduleHandler)
            repositoryUrls.add(url)
            clusterConfiguration.set("module-repositories", repositoryUrls)
            moduleHandler.repositories.add(repo)
            actor.sendMessage("§aRepository with url $url added!")
        }catch (e: Exception) {
            actor.sendMessage("§cRepository with url $url is not a valid repository!")
        }
    }

    @CommandSubPath("repository remove <url>")
    @CommandDescription("Remove a repository")
    fun repositoriesRemove(
        actor: ConsoleActor,
        @CommandParameter("url") url: String
    ) {
        val repositoryUrls = clusterConfiguration.getList<String>("module-repositories").toMutableList()
        if (!repositoryUrls.none { it.lowercase() == url.lowercase() }) {
            actor.sendMessage("§cRepository with url $url not found!")
            return
        }
        repositoryUrls.removeIf { it.lowercase() == url.lowercase() }
        clusterConfiguration.set("module-repositories", repositoryUrls)
        moduleHandler.repositories.removeIf { it.repoUrl.lowercase() == url.lowercase() }
        actor.sendMessage("§aRepository with url $url removed!")
    }

    @CommandSubPath("install <id>")
    @CommandDescription("Install a module")
    fun install(
        @CommandParameter("id", true, InstallableModulesSuggester::class) id: String
    ) = defaultScope.launch {
        moduleHandler.install(id, true)
    }

    @CommandSubPath("update <id>")
    @CommandDescription("Update a module")
    fun update(
        actor: ConsoleActor,
        @CommandParameter("id", true, InstalledModulesSuggester::class) id: String
    ) = defaultScope.launch {
        val targetRepository = moduleHandler.getRepository(id)
        if (targetRepository == null) {
            actor.sendMessage("§cModule with id $id has no repository!")
            return@launch
        }
        if (!targetRepository.isUpdateAvailable(id)) {
            actor.sendMessage("§cModule with id $id has no update available!")
            return@launch
        }
        actor.sendMessage("Updating module %hc%$id%tc%...")
        val file = moduleHandler.getModuleData(id)?.mapTo {
            if (it.loaded) it.file else null
        }
        if (file != null) moduleHandler.unloadModule(id)
        val latest = targetRepository.getLatestVersion(id)!!
        targetRepository.download(id, latest)
        actor.sendMessage("Module with id $id updated!")
        if (file != null) {
            moduleHandler.loadModule(file)
        }else {
            actor.sendMessage("Use 'module load $id' to load the module!")
        }
    }

    @CommandSubPath("uninstall <id>")
    @CommandDescription("Uninstall a module")
    fun uninstall(
        actor: ConsoleActor,
        @CommandParameter("id", true, UninstallableModulesSuggester::class) id: String
    ) = defaultScope.launch {
        moduleHandler.uninstall(id)
    }

}
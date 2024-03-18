package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.animation.impl.line.AnimatedLineAnimation
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.service.node.console.NodeConsole
import dev.redicloud.service.node.repository.node.LOGGER
import dev.redicloud.updater.Updater
import dev.redicloud.updater.suggest.BranchSuggester
import dev.redicloud.updater.suggest.BuildsSuggester
import dev.redicloud.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Command("version")
@CommandAlias(["ver"])
@CommandDescription("Displays the current version of the node service")
class VersionCommand(
    val console: NodeConsole
) : ICommand {

    @CommandSubPath("")
    @CommandDescription("Displays the current version of the node service")
    fun version(
        actor: ConsoleActor
    ) {
        actor.sendHeader("Version")
        actor.sendMessage("Version§8: %hc%$CLOUD_VERSION")
        actor.sendMessage("Git§8: %hc%$GIT")
        actor.sendMessage("Branch§8: %hc%$BRANCH")
        actor.sendMessage("CI-Build§8: %hc%$BUILD")
        actor.sendHeader("Version")
    }

    @CommandSubPath("checkupdate")
    @CommandDescription("Checks if an update is available")
    fun checkUpdate(
        actor: ConsoleActor
    ) = defaultScope.launch {
        if (BUILD == "local") {
            actor.sendMessage("You are running a local build, updates are not available!")
            return@launch
        }
        val updateInfo = Updater.updateAvailable()
        if (updateInfo.first && updateInfo.second != null) {
            actor.sendMessage("An update is available: %hc%${updateInfo.second}")
            actor.sendMessage("You can download the update with the command: %hc%version download $BRANCH ${updateInfo.second}")
            actor.sendMessage("And switch the update with the command: %hc%version switch $BRANCH ${updateInfo.second}")
        } else {
            actor.sendMessage("You are running the latest version!")
        }
    }

    @CommandSubPath("download [branch] [build]")
    @CommandDescription("Downloads a version")
    fun download(
        actor: ConsoleActor,
        @CommandParameter("branch", false, BranchSuggester::class) _branch: String?,
        @CommandParameter("build", false, BuildsSuggester::class) _build: String?
    ) = defaultScope.launch {
        val branch = _branch ?: BRANCH
        val build = _build ?: "latest"
        val buildId = if (build == "latest") {
            val projectInfo = Updater.getProjectInfo(branch)
            if (projectInfo == null) {
                actor.sendMessage("§cFailed to check for updates")
                return@launch
            }
            projectInfo.builds.maxOrNull() ?: run {
                actor.sendMessage("§cNo builds found for the branch ${toConsoleValue(branch, false)}!")
                return@launch
            }
        }else if (build.toIntOrNull() == null) {
            actor.sendMessage("§cInvalid build number")
            return@launch
        }else {
            build.toInt()
        }
        var canceled = false
        var error = false
        var downloaded = false
        val animation = AnimatedLineAnimation(
            console,
            200
        ) {
            if (canceled) {
                null
            } else if (downloaded) {
                canceled = true
                "Downloaded version ${toConsoleValue("$branch§8#%tc%$buildId")}§8: ${if (error) "§4✘" else "§2✓"}"
            } else {
                "Downloading version ${toConsoleValue("$branch§8#%tc%$buildId")}§8: %loading%"
            }
        }
        console.startAnimation(animation)
        try {
            val file = Updater.download(branch, buildId)
            downloaded = true
            actor.sendMessage("You can switch the version with the command: %hc%version switch $branch $buildId")
        }catch (e: Exception) {
            error = true
            actor.sendMessage("§cFailed to download the version!")
            LOGGER.severe("Failed to download the version", e)
        }
    }

    private val switchConfirms = mutableMapOf<Pair<String, String>, Long>()

    @CommandSubPath("switch [branch] [build]")
    @CommandDescription("Switch to a downloaded version")
    fun switch(
        actor: ConsoleActor,
        @CommandParameter("branch", false, BranchSuggester::class) _branch: String?,
        @CommandParameter("build", false, BuildsSuggester::class) _build: String?
    ) = defaultScope.launch {
        if (Updater.updateToVersion != null) {
            actor.sendMessage("§cAn update was already installed! Restart the node service to apply the changes!")
            return@launch
        }
        val branch = _branch ?: BRANCH
        val build = _build ?: "latest"
        if (BUILD == build && BRANCH == branch) {
            actor.sendMessage("You are already running this version!")
            return@launch
        }
        val buildId = if (build == "latest") {
            val projectInfo = Updater.getProjectInfo(branch)
            if (projectInfo == null) {
                actor.sendMessage("§cFailed to check for latest build! Make sure the branch exists!")
                return@launch
            }
            projectInfo.builds.maxOrNull() ?: run {
                actor.sendMessage("§cNo builds found for the branch ${toConsoleValue(branch, false)}!")
                return@launch
            }
        }else if (build.toIntOrNull() == null) {
            actor.sendMessage("§cInvalid build number")
            return@launch
        }else {
            build.toInt()
        }
        val installedVersions = Updater.localInstalledVersions()
        if (!installedVersions.containsKey(branch) || !installedVersions[branch]!!.contains(buildId)) {
            actor.sendMessage("§cThe version is not downloaded!")
            actor.sendMessage("§cYou can download the version with the command: %hc%version download <branch> <build>")
            return@launch
        }
        val confirmIdentifier = Pair(branch, build)
        if (branch.lowercase() != BRANCH.lowercase()
            && switchConfirms.getOrDefault(confirmIdentifier, 0) + 30000 < System.currentTimeMillis()) {
            actor.sendMessage("§cYou are trying to switch to a different branch!")
            actor.sendMessage("§cAre you sure you want to switch to the branch ${toConsoleValue("$branch#$build", false)}?")
            actor.sendMessage("§cThis can cause issues and data loss! Backup your data before switching is recommended!")
            actor.sendMessage("§cType the command again to confirm!")
            switchConfirms[confirmIdentifier] = System.currentTimeMillis()
            return@launch
        }
        if (branch == BRANCH && buildId < (BUILD.toIntOrNull() ?: -1)
            && switchConfirms.getOrDefault(confirmIdentifier, 0) + 30000 < System.currentTimeMillis()) {
            actor.sendMessage("§cYou are trying to switch to an older version!")
            actor.sendMessage("§cAre you sure you want to switch to the version ${toConsoleValue("$branch#$build", false)}?")
            actor.sendMessage("§cThis can cause issues and data loss! Backup your data before switching is recommended!")
            actor.sendMessage("§cType the command again to confirm!")
            switchConfirms[confirmIdentifier] = System.currentTimeMillis()
            return@launch
        }
        switchConfirms.remove(confirmIdentifier)
        val file = Updater.switchVersion(branch, buildId)
        actor.sendMessage("Activated the version: %hc%$branch§8#%tc%$buildId")
        actor.sendMessage("§cYou have to restart the node service to apply the changes!")
    }

    @CommandSubPath("branches")
    @CommandDescription("Displays all available branches")
    fun branches(
        actor: ConsoleActor
    ) = defaultScope.launch {
        val branches = Updater.getBranches().toMutableList()
        if (BRANCH == "local") {
            branches.add("local")
        }
        if (branches.isEmpty()) {
            actor.sendMessage("§cFailed to get the branches!")
            return@launch
        }
        actor.sendMessage("Available branches:")
        branches.forEach {
            if (it == BRANCH) {
                actor.sendMessage("§8- %hc%$it §7(§acurrent§7)")
            }else {
                actor.sendMessage("§8- %hc%$it")
            }
        }
    }

    @CommandSubPath("builds [branch]")
    @CommandDescription("Displays all available builds for a branch")
    fun builds(
        actor: ConsoleActor,
        @CommandParameter("branch", false, BranchSuggester::class) _branch: String?
    ) = defaultScope.launch {
        val branch = _branch ?: BRANCH
        val projectInfo = Updater.getProjectInfo(branch)
        if (projectInfo == null) {
            actor.sendMessage("§cFailed to get the builds! Make sure the branch exists!")
            return@launch
        }
        val builds = projectInfo.builds.map { it.toString() }.toMutableList()
        if (BRANCH == "local" && BUILD == "local") {
            builds.add("local")
        }
        if (builds.isEmpty()) {
            actor.sendMessage("§cNo builds found for the branch ${toConsoleValue(branch)}!")
            return@launch
        }
        actor.sendMessage("Available builds for branch ${toConsoleValue(branch)}:")
        builds.forEach {
            if (it == BUILD) {
                actor.sendMessage("§8- %hc%$it §7(§acurrent§7)")
            }else {
                actor.sendMessage("§8- %hc%$it")
            }
        }
    }

    @CommandSubPath("downloaded")
    @CommandDescription("Displays all downloaded versions")
    fun downloaded(
        actor: ConsoleActor
    ) = runBlocking {
        val installedVersions = Updater.localInstalledVersions()
        if (installedVersions.isEmpty()) {
            actor.sendMessage("No versions downloaded!")
            actor.sendMessage("Use the command ${toConsoleValue("version download <branch> <build>")} to download a version!")
            return@runBlocking
        }
        actor.sendMessage("Downloaded versions:")
        installedVersions.forEach { (branch, builds) ->
            actor.sendMessage("§8- %hc%$branch:")
            builds.forEach {
                if (it.toString() == BUILD && branch == BRANCH) {
                    actor.sendMessage("  §8➥ %tc%$it §7(§acurrent§7)")
                }else {
                    actor.sendMessage("  §8➥ %tc%$it")
                }
            }
        }
    }

}
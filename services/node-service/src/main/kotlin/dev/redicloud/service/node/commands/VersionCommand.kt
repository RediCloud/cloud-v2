package dev.redicloud.service.node.commands

import dev.redicloud.api.commands.*
import dev.redicloud.console.commands.ConsoleActor
import dev.redicloud.console.utils.toConsoleValue
import dev.redicloud.updater.Updater
import dev.redicloud.utils.*
import kotlinx.coroutines.runBlocking

@Command("version")
@CommandAlias(["ver"])
@CommandDescription("Displays the current version of the node service")
class VersionCommand : ICommand {

    @CommandSubPath("")
    @CommandDescription("Displays the current version of the node service")
    fun version(
        actor: ConsoleActor
    ) {
        actor.sendHeader("Version")
        actor.sendMessage("Version§8: %hc%${CLOUD_VERSION}")
        actor.sendMessage("Git§8: %hc%${if (DEV_BUILD) "dev" else "master"}@${GIT}")
        actor.sendMessage("CI-Build§8: %hc%${BUILD}")
        actor.sendHeader("Version")
    }

    @CommandSubPath("checkupdate")
    @CommandDescription("Checks if an update is available")
    fun checkUpdate(
        actor: ConsoleActor
    ) = runBlocking {
        if (BUILD == "local") {
            actor.sendMessage("You are running a local build, updates are not available!")
        }
        val updateInfo = Updater.updateAvailable()
        if (updateInfo.first && updateInfo.second != null) {
            actor.sendMessage("An update is available: %hc%${updateInfo.second}")
            actor.sendMessage("You can download the update with the command: %hc%version download $BRANCH ${updateInfo.second}")
            actor.sendMessage("And active the update with the command: %hc%version switch $BRANCH ${updateInfo.second}")
        } else {
            actor.sendMessage("You are running the latest version!")
        }
    }

    @CommandSubPath("download [branch] [build]")
    @CommandDescription("Downloads a version")
    fun download(
        actor: ConsoleActor,
        @CommandParameter("branch", false) _branch: String?,
        @CommandParameter("build", false) _build: String?
    ) = runBlocking {
        val branch = _branch ?: BRANCH
        val build = _build ?: "latest"
        val buildId = if (build == "latest") {
            val projectInfo = Updater.getProjectInfo(BRANCH)
            if (projectInfo == null) {
                actor.sendMessage("§cFailed to check for updates")
                return@runBlocking
            }
            projectInfo.builds.maxOrNull() ?: run {
                actor.sendMessage("§cNo builds found for the branch $branch!")
                return@runBlocking
            }
        }else if (build.toIntOrNull() == null) {
            actor.sendMessage("§cInvalid build number")
            return@runBlocking
        }else {
            build.toInt()
        }
        val file = Updater.download(branch, buildId)
        actor.sendMessage("Downloaded the version: %hc%$branch#$buildId")
        actor.sendMessage("You can activate the version with the command: %hc%version activate $branch $buildId")
    }

    @CommandSubPath("switch [branch] [build]")
    @CommandDescription("Switch to a downloaded version")
    fun switch(
        actor: ConsoleActor,
        @CommandParameter("branch", false) _branch: String?,
        @CommandParameter("build", false) _build: String?
    ) = runBlocking {
        val branch = _branch ?: BRANCH
        val build = _build ?: "latest"
        if (BUILD == build && BRANCH == branch) {
            actor.sendMessage("You are already running this version!")
            return@runBlocking
        }
        val buildId = if (build == "latest") {
            val projectInfo = Updater.getProjectInfo(BRANCH)
            if (projectInfo == null) {
                actor.sendMessage("§cFailed to check for latest build! Make sure the branch exists!")
                return@runBlocking
            }
            projectInfo.builds.maxOrNull() ?: run {
                actor.sendMessage("§cNo builds found for the branch $branch!")
                return@runBlocking
            }
        }else if (build.toIntOrNull() == null) {
            actor.sendMessage("§cInvalid build number")
            return@runBlocking
        }else {
            build.toInt()
        }
        val installedVersions = Updater.localInstalledVersions()
        if (!installedVersions.containsKey(branch) || !installedVersions[branch]!!.contains(buildId)) {
            actor.sendMessage("§cThe version is not downloaded!")
            actor.sendMessage("§cYou can download the version with the command: %hc%version download <branch> <build>")
            return@runBlocking
        }
        val file = Updater.activateVersion(branch, buildId)
        actor.sendMessage("Activated the version: %hc%$branch#$buildId")
        actor.sendMessage("§cYou have to restart the node service to apply the changes!")
    }

    @CommandSubPath("branches")
    @CommandDescription("Displays all available branches")
    fun branches(
        actor: ConsoleActor
    ) = runBlocking {
        val branches = Updater.getBranches().toMutableList()
        if (BRANCH == "local") {
            branches.add("local")
        }
        if (branches.isEmpty()) {
            actor.sendMessage("§cFailed to get the branches!")
            return@runBlocking
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
        @CommandParameter("branch", false) _branch: String?
    ) = runBlocking {
        val branch = _branch ?: BRANCH
        val projectInfo = Updater.getProjectInfo(branch)
        if (projectInfo == null) {
            actor.sendMessage("§cFailed to get the builds! Make sure the branch exists!")
            return@runBlocking
        }
        val builds = projectInfo.builds.map { it.toString() }.toMutableList()
        if (branch == "local" && BUILD == "local") {
            builds.add("local")
        }
        if (builds.isEmpty()) {
            actor.sendMessage("§cNo builds found for the branch $branch!")
            return@runBlocking
        }
        actor.sendMessage("Available builds for branch %hc%$branch:")
        builds.forEach {
            if (it.toString() == BUILD) {
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
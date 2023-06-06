package dev.redicloud.console.utils

import org.fusesource.jansi.AnsiConsole

class AnsiInstaller {

    companion object {
        var ansiSupported: Boolean? = null
    }

    fun install(force: Boolean = false): Boolean {
        if (ansiSupported != null && !force) return ansiSupported!!
        return try {
            AnsiConsole.systemInstall()
            ansiSupported = true
            true
        }catch (e: Exception) {
            ansiSupported = false
            false
        }
    }

}
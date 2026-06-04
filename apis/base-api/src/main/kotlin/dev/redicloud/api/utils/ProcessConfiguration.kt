package dev.redicloud.api.utils

import dev.redicloud.logging.LogManager
import dev.redicloud.utils.ConfigurationFileEditor
import dev.redicloud.utils.toConsoleValue
import java.io.File
import java.util.regex.Pattern

/**
 * Common process configuration shared by configuration templates, server versions,
 * and server version types. Defines JVM arguments, environment variables, program
 * parameters, default files to download, and file edits to apply before a server starts.
 *
 * Implementations include [CollectedProcessConfiguration] which merges multiple
 * configurations into one for a specific server start.
 */
interface ProcessConfiguration {

    /** Additional JVM arguments passed to the java command (e.g. `-Dfoo=bar`). */
    val jvmArguments: MutableList<String>

    /** Environment variables injected into the server process. */
    val environmentVariables: MutableMap<String, String>

    /** Program parameters appended after the jar path in the start command. */
    val programParameters: MutableList<String>

    /** Files to download before startup. Mapping of download URL to target path (relative to the server directory). */
    val defaultFiles: MutableMap<String, String>

    /**
     * Configuration file edits applied before startup.
     * Outer key: file path (relative to the server directory).
     * Inner map: configuration key to the desired value.
     */
    val fileEdits: MutableMap<String, MutableMap<String, String>>

    /** Optional list of referenced [ProcessConfiguration]s that were merged into this instance. */
    val references: List<ProcessConfiguration>? get() = null

    /**
     * Builds a list of [Pattern]s used to identify library files that should not be deleted
     * during server preparation. Includes patterns from [defaultFiles] keys and all [references].
     *
     * @param files additional file names to include in the pattern
     * @return compiled regex patterns matching library file names
     */
    fun getLibPatterns(vararg files: String): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        if (files.isNotEmpty()) {
            patterns.add(Pattern.compile("(${files.joinToString("|")})"))
        }
        if (defaultFiles.keys.isNotEmpty()) {
            patterns.add(Pattern.compile("(${defaultFiles.keys.joinToString("|")})"))
        }
        references?.forEach {
            patterns.addAll(it.getLibPatterns())
            if (it.defaultFiles.isNotEmpty()) {
                patterns.add(Pattern.compile("(${it.defaultFiles.keys.joinToString("|")})"))
            }
        }
        return patterns
    }

    companion object {
        private val logger = LogManager.logger(ProcessConfiguration::class)

        /**
         * Merges multiple [ProcessConfiguration]s into a single [CollectedProcessConfiguration].
         *
         * @param processConfigurations the configurations to merge
         * @return a collected configuration containing all values from the inputs
         */
        fun collect(vararg processConfigurations: ProcessConfiguration): ProcessConfiguration {
            return CollectedProcessConfiguration(processConfigurations.toList())
        }
    }
}

/**
 * Applies all [ProcessConfiguration.fileEdits] to files inside the given [folder].
 * Each entry maps a relative file path to a set of key-value edits. The file is parsed
 * by [ConfigurationFileEditor]; unsupported file formats or missing keys are logged as warnings.
 *
 * @param folder the server working directory that contains the files to edit
 * @param action optional transformation applied to each value before it is written (e.g. placeholder replacement)
 */
fun ProcessConfiguration.doFileEdits(folder: File, action: (String) -> String = { it }) {
    val logger = LogManager.logger(ProcessConfiguration::class)
    fileEdits.forEach { (file, editInfo) ->
        val fileToEdit = File(folder, file)
        if (!fileToEdit.exists()) {
            logger.warning("File $fileToEdit does not exist! So it can not be edited!")
            return@forEach
        }
        val editor = ConfigurationFileEditor.ofFile(fileToEdit)
        if (editor == null) {
            logger.warning(
                "§cFile ${toConsoleValue(fileToEdit, false)} is not a configuration file! So it can not be edited!"
            )
            return@forEach
        }
        editInfo.forEach { (key, value) ->
            try {
                editor.setValue(key, action(value))
            } catch (_: IllegalStateException) {
                logger.warning(
                    "§cKey ${toConsoleValue(
                        key,
                        false
                    )} does not exist in file ${toConsoleValue(fileToEdit, false)}!"
                )
            }
        }
        editor.saveToFile(fileToEdit)
    }
}

/**
 * A [ProcessConfiguration] that merges multiple configurations into one.
 * All JVM arguments, environment variables, program parameters, default files,
 * and file edits from the given [references] are flattened into single collections.
 *
 * @property references the source configurations that were merged
 */
class CollectedProcessConfiguration(
    override val references: List<ProcessConfiguration>
) : ProcessConfiguration {
    override val jvmArguments: MutableList<String> = references.flatMap { it.jvmArguments }.toMutableList()
    override val environmentVariables: MutableMap<String, String> = references.flatMap {
        it.environmentVariables.toList()
    }.toMap().toMutableMap()
    override val programParameters: MutableList<String> = references.flatMap { it.programParameters }.toMutableList()
    override val defaultFiles: MutableMap<String, String> = references.flatMap {
        it.defaultFiles.toList()
    }.toMap().toMutableMap()
    override val fileEdits: MutableMap<String, MutableMap<String, String>> = references.flatMap {
        it.fileEdits.toList()
    }.toMap().toMutableMap()
}

package cloud

import java.io.File

class EnvironmentLoader {

    private val environments = mutableMapOf(
        "LIBRARY_FOLDER" to File(".libs", RediCloud.cloudWorkingDirectory.absolutePath).absolutePath,
    )

    fun environments(): Map<String, String> {
        return environments
    }

}
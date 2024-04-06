package cloud

class EnvironmentLoader {

    private val environments = mutableMapOf(
        "LIBRARY_FOLDER" to "/libs"
    )

    fun environments(): Map<String, String> {
        return environments
    }

}
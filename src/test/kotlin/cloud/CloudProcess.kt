package cloud

import java.util.concurrent.TimeUnit

class CloudProcess(
    cloudFileCopier: CloudFileCopier,
    debugPort: Int? = null,
    environmentLoader: EnvironmentLoader
) {

    private val builder: ProcessBuilder
    private var process: Process? = null

    init {
        val java = System.getProperty("redicloud.java", "java")

        val command = cloudFileCopier.startFile.readText().substringAfter("java")

        val startCommand = mutableListOf(java)
        if (debugPort != null) {
            startCommand.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$debugPort")
        }
        startCommand.addAll(command.split(" "))
        builder = ProcessBuilder(
            startCommand
        )
        builder.environment().putAll(environmentLoader.environments())
    }

    fun isAlive(): Boolean {
        return process?.isAlive == true
    }

    fun start() {
        process = builder.start()
    }

    fun stop() {
        if (process == null) {
            return
        }
        execute("stop")
        Thread.sleep(500)
        execute("stop")
        if (!process!!.waitFor(10, TimeUnit.SECONDS)) {
            println("Process did not stop in time. Destroying it.")
            process!!.destroyForcibly()
        }
    }

    fun execute(command: String) {
        process?.outputStream?.write(command.toByteArray())
        process?.outputStream?.flush()
    }

}
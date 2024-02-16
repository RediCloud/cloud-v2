package dev.redicloud.service.node.console

import dev.redicloud.api.utils.DATABASE_JSON
import dev.redicloud.api.utils.NODE_JSON
import dev.redicloud.console.Console
import dev.redicloud.console.jline.ConsoleQuestion
import dev.redicloud.console.jline.ConsoleQuestionCondition
import dev.redicloud.console.jline.ask
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.config.DatabaseConfiguration
import dev.redicloud.database.config.DatabaseNode
import dev.redicloud.database.config.toFile
import dev.redicloud.logging.LogManager
import dev.redicloud.logging.getLogLevelByProperty
import dev.redicloud.repository.java.version.getJavaVersion
import dev.redicloud.repository.java.version.isJavaVersionNotSupported
import dev.redicloud.repository.java.version.isJavaVersionSupported
import dev.redicloud.service.node.NodeConfiguration
import dev.redicloud.utils.*
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.logging.Filter
import java.util.logging.Level

class InitializeConsole() : Console(
    "unknown", null, logLevel = getLogLevelByProperty() ?: Level.SEVERE, uninstallAnsiOnClose = false
) {

    companion object {
        private val logger = LogManager.logger(InitializeConsole::class)
    }

    var firstStartDetected = false

    private val nodeNameQuestion = ConsoleQuestion(
        question = "What should be the name of this node?",
        condition = object : ConsoleQuestionCondition {
            override fun fail(input: String): Boolean {
                if (input.contains(" ")) {
                    writeLine("§cThe name of the node can't contain spaces!")
                    return true
                }
                if (input.length > 16) {
                    writeLine("§cThe name of the node can't be longer than 32 characters!")
                    return true
                }
                return false
            }
        },
        completer = listOf(
            "node01",
            "node02",
            "node03",
            "node04",
            "node05",
            "node06",
            "node07",
            "node08",
            "node09",
            "node10"
        )
    )

    private val nodeAddressQuestion = ConsoleQuestion(
        question = "What should be the host address of this node?",
        condition = object : ConsoleQuestionCondition {
            override fun fail(input: String): Boolean {
                val fail = input.contains(" ")
                if (fail) writeLine("§cThe host address of the node can't contain spaces!")
                return fail
            }
        },
        completer = getAllIpV4()
    )

    private val databasePasswordQuestion = ConsoleQuestion(
        question = "What is the password of the database?",
        condition = object : ConsoleQuestionCondition {
            override fun fail(input: String): Boolean {
                val fail = input.contains(" ")
                if (fail) writeLine("§cThe password of the database can't contain spaces!")
                return fail
            }
        }
    )

    private val databaseIdQuestion = ConsoleQuestion(
        question = "What is the id of the database?",
        condition = object : ConsoleQuestionCondition {
            override fun fail(input: String): Boolean {
                val fail = input.toIntOrNull() == null
                if (fail) writeLine("§cThe id of the database must be a number!")
                return fail
            }
        }
    )

    private val databaseNodeQuestion = ConsoleQuestion(
        question = "What is the address and port of the node? (e.g. 127.0.0.1:6379)",
        condition = object : ConsoleQuestionCondition {
            override fun fail(input: String): Boolean {
                if (input.split(":").size != 2) {
                    writeLine("§cThe address and port of the node must be in the format of 'host:port'")
                    return true
                }
                val ip = input.split(":")[0]
                val port = input.split(":")[1].toIntOrNull()
                if (port == null) {
                    writeLine("§cThe port of the node must be a number!")
                    return true
                }
                if (!isIpv4(ip)) {
                    writeLine("§cThe host address of the node must be a valid ip address!")
                    return true
                }
                return false
            }
        }
    )

    private val databaseSSLQuestion = ConsoleQuestion(
        question = "Use SSL to connect to the redis? (yes/no)",
        condition = object : ConsoleQuestionCondition {
            override fun fail(input: String): Boolean {
                val fail =
                    input != "yes" && input != "no" && input != "y" && input != "n" && input != "true" && input != "false"
                if (fail) writeLine("§cThe answer must be either 'yes' or 'no'!")
                return fail
            }
        }
    )

    private val databaseAddNodeQuestion = ConsoleQuestion(
        question = "Do you want to add another node? This is only needed if you have a redis cluster! (yes/no)"
    )

    private val databaseNodeTokenQuestion = ConsoleQuestion(
        question = "Do you have a cluster token for the cloud cluster? (yes/no)\n" +
                "If you don´t have one, you can create one with the command 'token create' in a other cloud node console.\n" +
                "Or you can type 'no' and enter the redis credentials manually.",
    )

    internal var serviceId: ServiceId? = null
    internal var nodeConfiguration: NodeConfiguration? = null
    internal var databaseConfiguration: DatabaseConfiguration? = null
    internal var databaseConnection: DatabaseConnection? = null

    init {
        disableCommands()
        lineFormat = "\t%message%"
        emptyPrompt()
        updatePrompt()
        sendHeader()
        LogManager.rootLogger().filter = Filter { record
            -> record.level != Level.INFO && !record.message.contains("org.redisson") && !record.message.contains("SLF4J:") }
        runBlocking {
            nodeConfiguration = checkNode()
            serviceId = nodeConfiguration!!.toServiceId()
            databaseConnection = checkDatabase(serviceId!!)
            writeLine("§8» §fStarting ...")
        }
    }

    override fun sendHeader() {
        super.sendHeader()
        getJavaVersion() // Load versions
        writeLine("§8» §fChecks§8:")
        writeLine("§f‾‾‾‾‾‾‾‾‾‾‾‾‾")
        writeLine("§8• §fLibraries §8» ${checkLibs()}")
        writeLine("§8• §fNo-Root-User §8» ${checkUser()}")
        writeLine("§8• §fJava-Version §8» ${checkJava()}")
        writeLine("§8• §fLog-Level §8» %hc%${logger.level.name}")
        writeLine("")
        writeLine("")
    }

    private fun checkLibs(): String {
        return try {
            Class.forName("org.redisson.Redisson")
            "§2✓ §8(§fLibs %hc%loaded§8)"
        } catch (e: ClassNotFoundException) {
            "§4✘ §8§l(§c§lMissing libs!§8§l)"
        }
    }

    private fun checkUser(): String {
        return if (USER_NAME == "root") {
            "§e✘ §8(§cRoot user detected!§8)"
        } else {
            "§2✓ §8(§fUser: %hc%$USER_NAME§8)"
        }
    }

    private fun checkJava(): String {
        return if (isJavaVersionSupported(getJavaVersion())) {
            "§2✓ §8(§fJava: %hc%${System.getProperty("java.version")}§8)"
        } else if (isJavaVersionNotSupported(getJavaVersion())) {
            "§e§l~ §8(§eJava: ${System.getProperty("java.version")}§8| §enot tested§8)"
        } else {
            "§4✘ §8(§cJava: ${System.getProperty("java.version")}§8| §cnot supported§8)"
        }
    }

    private suspend fun checkNode(): NodeConfiguration {
        val nodeFile = NODE_JSON.getFile()
        if (!nodeFile.exists()) {
            writeLine("Node file not found! Starting node setup in 5 seconds...")
            Thread.sleep(5000)
            return nodeSetup()
        }
        return try {
            val config = NodeConfiguration.fromFile(nodeFile)
            writeLine("§8» §fNode Information§8:")
            writeLine("§f‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾")
            writeLine("§8• §fName §8» %hc%${config.nodeName}")
            writeLine("§8• §fHost-Address §8» %hc%${config.hostAddress}")
            writeLine("§8• §fID §8» %hc%${config.uniqueId}")
            writeLine("")
            writeLine("")
            config
        } catch (e: Exception) {
            writeLine("§cError while reading node file! Starting node setup in 5 seconds...")
            Thread.sleep(5000)
            nodeSetup()
        }
    }

    private suspend fun nodeSetup(): NodeConfiguration {
        firstStartDetected = true
        if (getCurrentScreen().name == "node-setup") {
            clearScreen()
        }else {
            switchScreen(createScreen("node-setup"))
        }
        clearScreen()
        writeLine("")
        writeLine("")
        writeLine("    §8» §fNode Setup§8:")
        writeLine("    §f‾‾‾‾‾‾‾‾‾‾‾‾‾")
        writeLine("")
        prompt = "\t\t%hc%» %tc%"
        updatePrompt()
        val nodeName: String = nodeNameQuestion.ask(this)
        writeLine("")
        writeLine("")
        val hostAddress: String = nodeAddressQuestion.ask(this)
        writeLine("")
        writeLine("")
        val uniqueId = UUID.randomUUID()
        val config = NodeConfiguration(nodeName, uniqueId, hostAddress)
        NODE_JSON.create()
        config.toFile(NODE_JSON.getFile())
        switchToDefaultScreen()
        emptyPrompt()
        writeLine("You finished the node setup!")
        Thread.sleep(2000)
        return config
    }

    private suspend fun checkDatabase(serviceId: ServiceId): DatabaseConnection {
        emptyPrompt()
        val databaseFile = DATABASE_JSON.getFile()
        if (!databaseFile.exists()) {
            writeLine("Database file not found! Starting database setup in 5 seconds...")
            Thread.sleep(5000)
            return databaseSetup()
        }
        try {
            databaseConfiguration = DatabaseConfiguration.fromFile(databaseFile)
            val p = testDatabase(databaseConfiguration!!, serviceId)
            val error = p.second
            if (error != null) {
                switchScreen(getScreen("database-test") ?: createScreen("database-test"))
                clearScreen()
                logger.severe("§cError while testing database connection!", error)
                prompt = "\t\t%hc%» %tc%"
                updatePrompt()
                val databaseSetup: Boolean = ask("Do you want to start the database setup again? (yes/no)")
                if (databaseSetup) {
                    return databaseSetup()
                }
                emptyPrompt()
                writeLine("Retrying in 10 seconds...")
                Thread.sleep(10000)
                return checkDatabase(serviceId)
            }
            return p.first
        } catch (e: Exception) {
            writeLine("§cError while reading database file! Starting database setup in 5 seconds...")
            logger.log(Level.FINE, "Reading file error: ${databaseFile.absolutePath}", e)
            Thread.sleep(5000)
            return databaseSetup()
        }
    }

    private suspend fun testDatabase(config: DatabaseConfiguration, serviceId: ServiceId): Pair<DatabaseConnection, Throwable?> {
        emptyPrompt()
        val connection = DatabaseConnection(config, serviceId)
        return try {
            connection.connect()
            connection to null
        } catch (e: Exception) {
            connection to e
        }
    }

    private suspend fun databaseSetup(): DatabaseConnection {
        firstStartDetected = true
        if (getCurrentScreen().name == "database-setup") {
            clearScreen()
        }else {
            switchScreen(createScreen("database-setup"))
        }
        writeLine("")
        writeLine("")
        writeLine("    §8» §fDatabase Setup§8:")
        writeLine("    §f‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾")
        writeLine("")
        prompt = "\t\t%hc%» %tc%"
        updatePrompt()
        val useToken: Boolean = databaseNodeTokenQuestion.ask(this)
        if (useToken) {
            writeLine("§cIts currently not possible to use a token!")
            writeLine("Please enter your redis credentials manually!")
            Thread.sleep(2000)
        }
        val password: String = databasePasswordQuestion.ask(this)
        val nodes = mutableListOf<String>(databaseNodeQuestion.ask(this))
        while (databaseAddNodeQuestion.ask(this)) {
            nodes.add(databaseNodeQuestion.ask(this))
        }
        var databaseId = 0
        if (nodes.size == 1) {
            databaseId = databaseIdQuestion.ask(this)
        }
        val ssl = databaseSSLQuestion.ask<Boolean>(this)
        writeLine("")
        writeLine("")
        val config = DatabaseConfiguration(password, nodes.map {
            DatabaseNode(it.split(":")[0], it.split(":")[1].toInt(), ssl)
        }, databaseId)
        DATABASE_JSON.create()
        config.toFile(DATABASE_JSON.getFile())
        switchToDefaultScreen()
        emptyPrompt()
        writeLine("You finished the database setup!")
        Thread.sleep(2000)
        return checkDatabase(ServiceId(UUID.randomUUID(), ServiceType.NODE))
    }

}
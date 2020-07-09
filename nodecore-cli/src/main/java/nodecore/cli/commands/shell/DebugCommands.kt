package nodecore.cli.commands.shell

import nodecore.cli.cliCommand
import nodecore.cli.prepareResult
import org.jutils.jprocesses.JProcesses
import org.koin.ext.isInt
import org.veriblock.core.utilities.DiagnosticInfo
import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.core.failure
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.nio.file.Paths

fun CommandFactory.debugCommands() {
    cliCommand(
        name = "Get Debug Information",
        form = "getdebuginfo",
        description = "Collect information about the application for troubleshooting",
        parameters = listOf(
            CommandParameter(name = "network", mapper = CommandParameterMappers.STRING, required = false),
            CommandParameter(name = "networkDataFolder", mapper = CommandParameterMappers.STRING, required = false), // NodeCore network data folder
            CommandParameter(name = "nodecoreFolder", mapper = CommandParameterMappers.STRING, required = false) // NodeCore root folder
        )
    ) {
        printInfo("Running several checks, this may take a few moments...")

        // Get the network
        val network = getOptionalParameter<String>("network")?.toLowerCase() ?: "mainnet"
        // Verify the network parameter
        if (network != "mainnet" && network != "testnet" && network != "alpha" ) {
            return@cliCommand failure("V004", "Unknown Network", "The supplied network $network is not valid, please use mainnet, testnet or alpha.")
        }
        printInfo("Detected network: $network")

        // Get the NodeCore folder
        val providedNodeCoreFolder = getOptionalParameter<String>("nodecoreFolder") ?: getDefaultNodeCoreFolder()
        val nodecoreFolder = File(providedNodeCoreFolder)
        if (!nodecoreFolder.exists()) {
            return@cliCommand failure("V004", "Unable to find the NodeCore folder", "The NodeCore folder $nodecoreFolder doesn't exists.")
        }
        printInfo("Detected NodeCore folder: $providedNodeCoreFolder")

        // Get the data folder
        val providedNetworkDataFolder = getOptionalParameter<String>("networkDataFolder") ?: Paths.get(providedNodeCoreFolder, "bin", network).toString()
        val networkDataFolder = File(providedNetworkDataFolder)
        if (!networkDataFolder.exists()) {
            return@cliCommand failure("V004", "Unable to find the network data folder", "The network data folder $networkDataFolder doesn't exists.")
        }
        printInfo("Detected NodeCore data folder: $providedNetworkDataFolder")

        // Get the default diagnostic information
        val diagnosticInfo = DiagnosticUtility.getDiagnosticInfo()
        // Get the system environment variables related with NodeCore
        val nodecoreEnvironmentVariables = try {
            System.getenv().filter {
                it.key.toLowerCase().contains("nodecore")
            }.map {
                NodecoreEnvironmentVariables(it.key, it.value)
            }
        } catch (ignored: SecurityException) {
            null
        }

        val configuration = ArrayList<String>()
        val nodeCoreDataFileTree = networkDataFolder.walk().filter { file ->
            !file.parentFile.name.isInt()
        }.map { file ->
            // Get the content from the configuration file
            if (file.name == "nodecore.properties") {
                file.forEachLine { line ->
                    // Don't add the password-related configurations
                    if (!line.contains("password", true)) {
                        configuration.add(line)
                    }
                }
            }
            file.path.replace(networkDataFolder.path, "")
        }.toList()

        // Get the process information related with NodeCore
        val processInformation = JProcesses.getProcessList().filter { process ->
            process.name.contains("nodecore", true) || process.name.contains("veriblock", true) ||
                process.command.contains("nodecore", true) || process.command.contains("veriblock", true)
        }.map { process ->
            ProcessInformation(process.pid, process.name, process.command)
        }
        // Verify if the common NodeCore ports are available
        val ports = listOf(6500, 7500, 7501, 8080, 8081, 8500, 10500, 10501, 10502)
        val portInformation = ports.map { port ->
            try {
                val socket = ServerSocket(port)
                socket.close()
                PortInformation(port, true)
            } catch (ignored: IOException) {
                PortInformation(port, false)
            }
        }
        // Generate the final object with all the collected information
        val debugInformation = DebugInformation(
            diagnosticInfo,
            nodeCoreDataFileTree,
            nodecoreEnvironmentVariables,
            processInformation,
            portInformation,
            configuration
        )
        prepareResult(true, emptyList()) {
            debugInformation
        }
    }
}

fun getDefaultNodeCoreFolder(): String {
    // Get the root folder from the package
    val packageParentFolder = Paths.get("").toAbsolutePath().parent?.parent
        ?: error("Unable to find the default root NodeCore folder, please specify it as a parameter.")
    // Search the NodeCore folder inside the package
    val nodeCoreFolder = packageParentFolder.toFile().listFiles().find {
        it.name.startsWith("nodecore-0") // nodecore-0.x.x...
    } ?: error("Unable to find the default root NodeCore folder, please specify it as a parameter.")
    return nodeCoreFolder.toString()
}

data class DebugInformation(
    val diagnosticInfo: DiagnosticInfo,
    val nodecoreDataFileTree: List<String>,
    val nodecoreEnvironmentVariables: List<NodecoreEnvironmentVariables>?,
    val nodecoreProcesses: List<ProcessInformation>,
    val nodecorePorts: List<PortInformation>,
    val configuration: List<String>
)

data class PortInformation(
    val id: Int,
    val isFree: Boolean
)

data class ProcessInformation(
    val pid: String?,
    val name: String?,
    val command: String?
)

data class NodecoreEnvironmentVariables(
    val key: String,
    val value: String
)

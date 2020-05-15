package nodecore.cli.commands.shell

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import nodecore.cli.cliCommand
import org.apache.commons.codec.digest.DigestUtils
import org.veriblock.core.utilities.DiagnosticInfo
import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.URL

fun CommandFactory.debugCommands() {
    cliCommand(
        name = "Get Debug Information",
        form = "getdebuginfo",
        description = "Collect information about the application for troubleshooting",
        parameters = listOf(
            CommandParameter(name = "dataFolder", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "nodecoreFolder", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "network", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        // Get the supplied network parameter
        val network = getParameter<String>("network").toLowerCase()
        // Verify the network parameter
        if (network != "mainnet" && network != "testnet" && network != "alpha" ) {
            return@cliCommand failure("V004", "Unknown Network", "The supplied network $network is not valid, please use mainnet, testnet or alpha.")
        }
        // Get the data folder provided by the user
        val dataFolder = File(getParameter<String>("dataFolder"))
        if (!dataFolder.exists()) {
            return@cliCommand failure("V004", "Unable to find the data folder", "The supplied data folder $dataFolder doesn't exists.")
        }
        // Get the nodecore folder provided by the user
        val nodecoreFolder = File(getParameter<String>("nodecoreFolder"))
        if (!nodecoreFolder.exists()) {
            return@cliCommand failure("V004", "Unable to find the nodecore folder", "The supplied nodecore folder $nodecoreFolder doesn't exists.")
        }
        // Get the bootstrap information
        val result = try {
            URL("https://mirror.veriblock.org/bootstrap/$network/blockFiles.json").readText()
        } catch (ignored: Exception) {
            null
        }
        // Parse the bootstrap information
        val blockList = try {
            Gson().fromJson(result, BlockList::class.java)
        } catch (ignored: JsonSyntaxException) {
            null
        }
        // Get the default diagnostic information
        val diagnosticInfo = DiagnosticUtility.getDiagnosticInfo()
        // Get the system environment variables related with NodeCore
        val nodecoreEnvironmentVariables = try {
            System.getenv().filter {
                it.key.toLowerCase().contains("nodecore")
            }.map {
                NodecoreEnvironmentVariables(it.key, it.value)
            }
        } catch(ignored: SecurityException) {
            null
        }

        val configuration = ArrayList<String>()

        // Check all the files inside the data folder, and verify the file integrity
        val nodecoreDataInformation = dataFolder.walk().filter {
            it.absolutePath.toLowerCase().contains(network) || it.absolutePath == dataFolder.absolutePath || it.name == "nodecore.properties"
        }.map { file ->
            val calculateFileSpecifications = file.name == "nodecore.dat" || file.parentFile?.parentFile?.name == "blocks"
            val fileSpecifications = if (calculateFileSpecifications) {
                file.inputStream().use { inputStream ->
                    val fileLength = file.length()
                    val fileChecksum = DigestUtils.md5Hex(inputStream)
                    val blockFileData = blockList?.files?.find { blockFile ->
                        (blockFile.name == file.name && blockFile.folder == "${file.parentFile?.parentFile?.name}/${file.parentFile?.name}") ||
                            (blockFile.name == "nodecore.dat" && file.name == "nodecore.dat")
                    }
                    val fileState = blockFileData?.let { blockFile ->
                        if (fileLength != blockFile.size || fileChecksum != blockFile.checksum) {
                            "Outdated - Length: $fileLength (expected: ${blockFile.size}), checksum: $fileChecksum (expected: ${blockFile.checksum})"
                        } else {
                            "Ok"
                        }
                    } ?: "Unknown"
                    FileSpecifications(fileLength, fileChecksum, fileState)
                }
            } else {
                null
            }
            // Get the content from the configuration file
            if (file.name == "nodecore.properties") {
                file.forEachLine { line ->
                    // Don't add the password-related configurations
                    if (!line.toLowerCase().contains("password")) {
                        configuration.add(line)
                    }
                }
            }
            FileInformation(file.name, file.absolutePath, fileSpecifications)
        }.toList()

        // Check the libraries inside the nodecore folder
        val nodecoreLibraryInformation = nodecoreFolder.walk().filter {
            it.name.toLowerCase().endsWith(".jar")
        }.map { file ->
            FileInformation(file.name, file.absolutePath)
        }.toList()

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
            nodecoreDataInformation,
            nodecoreLibraryInformation,
            nodecoreEnvironmentVariables,
            portInformation,
            configuration
        )
        val jsonDebugInformation = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(debugInformation)
        // Display all the information to the user
        shell.printInfo(jsonDebugInformation)
        // Store the output into a file
        val outputFile = File("${dataFolder.toPath()}/getdebuginfo.json")
        outputFile.writeText(
            jsonDebugInformation
        )
        shell.printInfo("The output file has been stored at: ${outputFile.toPath()}")
        success()
    }
}

data class DebugInformation(
    val diagnosticInfo: DiagnosticInfo,
    val nodecoreDataInformation: List<FileInformation>,
    val nodecoreLibraryInformation: List<FileInformation>,
    val nodecoreEnvironmentVariables: List<NodecoreEnvironmentVariables>?,
    val nodecorePorts: List<PortInformation>,
    val configuration: List<String>
)

data class PortInformation(
    val id: Int,
    val isFree: Boolean
)

data class FileInformation(
    val name: String,
    val route: String,
    val fileSpecifications: FileSpecifications? = null
)

data class FileSpecifications(
    val length: Long,
    val checksum: String,
    val state: String
)

data class NodecoreEnvironmentVariables(
    val key: String,
    val value: String
)

data class BlockList(
    val updateDate: String,
    val files: List<BlockFile>
)

data class BlockFile(
    val name: String,
    val folder: String,
    val size: Long,
    val checksum: String
)

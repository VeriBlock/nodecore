package nodecore.cli.commands.rpc

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.PoolStatePayload
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import java.io.UnsupportedEncodingException

fun CommandFactory.poolCommands() {
    rpcCommand(
        name = "Stop Pool",
        form = "stoppool|stopsolopool",
        description = "Stops the built-in pool service in NodeCore"
    ) {
        val request = VeriBlockMessages.StopPoolRequest.newBuilder().build()
        val result = cliShell.adminService.stopPool(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Start Solo Pool",
        form = "startsolopool",
        description = "Starts the built-in pool service in NodeCore in solo mode",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("stoppool", "getbalance", "setdefaultaddress") }
    ) {
        val address: String? = getParameter("address")
        val request = if (address != null) {
            VeriBlockMessages.StartSoloPoolRequest.newBuilder()
                .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                .build()
        } else {
            VeriBlockMessages.StartSoloPoolRequest.newBuilder().build()
        }
        val result = cliShell.adminService.startSoloPool(request)

        prepareResult(result.success, result.resultsList) {
            printInfo(
                "By default, your pool homepage will be available in a web browser at:\n" +
                    "\thttp://127.0.0.1:8500\n" +
                    "And a VeriBlock Proof-of-Work (PoW) miner can be pointed to:\n" +
                    "\t127.0.0.1:8501\n" +
                    "Remember that by default a solo pool mines to your default address!\n" +
                    "You can view your default address with the command: " +
                    "getinfo"
            )
        }
    }

    rpcCommand(
        name = "Start Pool",
        form = "startpool",
        description = "Starts the built-in pool service in NodeCore",
        parameters = listOf(
            CommandParameter(name = "type", mapper = CommandParameterMappers.STRING, required = true)
        ),
        suggestedCommands = { listOf("stoppool", "getbalance") }
    ) {
        val request = VeriBlockMessages.StartPoolRequest.newBuilder().apply {
            try {
                type = ByteString.copyFrom(getParameter<String>("type").toByteArray(charset("UTF-8")))
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }.build()
        val result = cliShell.adminService.startPool(request)

        prepareResult(result.success, result.resultsList) {
            printInfo(
                "By default, your pool homepage will be available in a web browser at:\n" +
                    "\thttp://127.0.0.1:8500\n" +
                    "And a VeriBlock Proof-of-Work (PoW) miner can be pointed to:\n" +
                    "\t127.0.0.1:8501\n" +
                    "Remember that by default a regular pool sends coins to each miner,\n" +
                    "and requires that each miner use a valid VBK address as their username!\n" +
                    "You can view your default address with the command: " +
                    "getinfo"
            )
        }
    }

    rpcCommand(
        name = "Get Pool Info",
        form = "getpoolinfo",
        description = "Returns pool configuration and metrics",
        suggestedCommands = { listOf("getinfo", "getstateinfo", "startpool", "stoppool") }
    ) {
        val request = VeriBlockMessages.GetPoolStateRequest.newBuilder().build()
        val result = cliShell.adminService.getPoolState(request)

        prepareResult(result.success, result.resultsList) {
            PoolStatePayload(result)
        }
    }

    rpcCommand(
        name = "Restart Pool Web Server",
        form = "restartpoolwebserver",
        description = "Restarts the built-in pool web server"
    ) {
        val request = VeriBlockMessages.RestartPoolWebServerRequest.newBuilder().build()
        val result = cliShell.adminService.restartPoolWebServer(request)

        prepareResult(result.success, result.resultsList)
    }
}

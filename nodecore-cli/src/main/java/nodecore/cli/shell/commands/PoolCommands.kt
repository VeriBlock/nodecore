package nodecore.cli.shell.commands

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.cli.commands.DefaultCommandContext
import nodecore.cli.commands.serialization.EmptyPayload
import nodecore.cli.commands.serialization.FormattableObject
import nodecore.cli.commands.serialization.PoolStatePayload
import org.veriblock.shell.Command
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.io.UnsupportedEncodingException

fun Shell.poolCommands(
    context: DefaultCommandContext
) {
    command(
        name = "Stop Pool",
        form = "stoppool",
        description = "Stops the built-in pool service in NodeCore"
    ) {
        try {
            context.adminService().stopPool(VeriBlockMessages.StopPoolRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Start Solo Pool",
        form = "startsolopool",
        description = "Starts the built-in pool service in NodeCore in solo mode",
        parameters = listOf(
            CommandParameter(name = "address", type = CommandParameterType.STANDARD_ADDRESS, required = false)
        )
    ) {
        try {
            val address: String? = getParameter("address")
            val reply = if (address != null) {
                context.adminService()
                    .startSoloPool(VeriBlockMessages.StartSoloPoolRequest.newBuilder()
                        .setAddress(ByteStringAddressUtility.createProperByteStringAutomatically(address))
                        .build())
            } else {
                context.adminService()
                    .startSoloPool(VeriBlockMessages.StartSoloPoolRequest.newBuilder().build())
            }
            val suggestedCommands = listOf(
                getCommand("stoppool"),
                getCommand("getbalance"),
                getCommand("setdefaultaddress")
            )

            reply.toShellResult(context, suggestedCommands)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Start Pool",
        form = "startpool",
        description = "Starts the built-in pool service in NodeCore",
        parameters = listOf(
            CommandParameter(name = "type", type = CommandParameterType.STRING, required = true)
        )
    ) {
        try {
            val builder = VeriBlockMessages.StartPoolRequest.newBuilder()
            val type: String = getParameter("type")

            try {
                builder.type = ByteString.copyFrom(type.toByteArray(charset("UTF-8")))
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            val suggestedCommands = listOf(
                getCommand("stoppool"),
                getCommand("getbalance")
            )

            context.adminService().startPool(builder.build())
                .toShellResult(context, suggestedCommands)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Get Pool Info",
        form = "getpoolinfo",
        description = "Returns pool configuration and metrics"
    ) {
        try {
            val suggestedCommands = listOf(
                getCommand("getinfo"),
                getCommand("getstateinfo"),
                getCommand("startpool"),
                getCommand("stoppool")
            )

            context.adminService()
                .getPoolState(VeriBlockMessages.GetPoolStateRequest.newBuilder().build())
                .toShellResult(context, suggestedCommands)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Restart Pool Web Server",
        form = "restartpoolwebserver",
        description = "Restarts the built-in pool web server"
    ) {
        try {
            context.adminService()
                .restartPoolWebServer(VeriBlockMessages.RestartPoolWebServerRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }
}

fun VeriBlockMessages.StopPoolReply.toShellResult(
    context: DefaultCommandContext
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<EmptyPayload>(resultsList)
        temp.success = true
        temp.payload = EmptyPayload()
        context.outputObject(temp)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.GetPoolStateReply.toShellResult(
    context: DefaultCommandContext,
    suggestedCommands: List<Class<out Command>>? = emptyList()
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<PoolStatePayload>(resultsList)
        temp.success = true
        temp.payload = PoolStatePayload(this)
        context.outputObject(temp)
        context.suggestCommands(suggestedCommands)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.RestartPoolWebServerReply.toShellResult(
    context: DefaultCommandContext
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<EmptyPayload>(resultsList)
        temp.success = true
        temp.payload = EmptyPayload()
        context.outputObject(temp)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.StartSoloPoolReply.toShellResult(
    context: DefaultCommandContext,
    suggestedCommands: List<Class<out Command>>? = emptyList()
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<EmptyPayload>(resultsList)
        temp.success = true
        temp.payload = EmptyPayload()
        context.outputObject(temp)
        val infoMessage = "By default, your pool homepage will be available in a web browser at:\n" +
            "\thttp://127.0.0.1:8500\n" +
            "And a VeriBlock Proof-of-Work (PoW) miner can be pointed to:\n" +
            "\t127.0.0.1:8501\n" +
            "Remember that by default a solo pool mines to your default address!\n" +
            "You can view your default address with the command: " +
            "getinfo"
        context.write().info(infoMessage)
        context.suggestCommands(suggestedCommands)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.StartPoolReply.toShellResult(
    context: DefaultCommandContext,
    suggestedCommands: List<Class<out Command>>? = emptyList()
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<EmptyPayload>(resultsList)
        temp.success = true
        temp.payload = EmptyPayload()
        context.outputObject(temp)
        val infoMessage = "By default, your pool homepage will be available in a web browser at:\n" +
            "\thttp://127.0.0.1:8500\n" +
            "And a VeriBlock Proof-of-Work (PoW) miner can be pointed to:\n" +
            "\t127.0.0.1:8501\n" +
            "Remember that by default a regular pool sends coins to each miner,\n" +
            "and requires that each miner use a valid VBK address as their username!\n" +
            "You can view your default address with the command: " +
            "getinfo"
        context.write().info(infoMessage)
        context.suggestCommands(suggestedCommands)
        success {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

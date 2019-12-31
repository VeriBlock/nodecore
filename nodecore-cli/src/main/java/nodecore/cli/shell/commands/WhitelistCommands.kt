package nodecore.cli.shell.commands

import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.commands.DefaultCommandContext
import nodecore.cli.commands.serialization.FormattableObject
import nodecore.cli.commands.serialization.ListAllowedPayload
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun Shell.allowedCommands(
    context: DefaultCommandContext
) {
    command(
        name = "Add Whitelist Address",
        form = "addallowed",
        description = "Add allowed addresses",
        parameters = listOf(
            CommandParameter(name = "address", type = CommandParameterType.STRING, required = true)
        )
    ) {
        val value: String = getParameter("address")
        try {
            context.adminService()
                .setAllowed(VeriBlockMessages.SetAllowedRequest.newBuilder()
                    .setCommand(VeriBlockMessages.SetAllowedRequest.Command.ADD)
                    .setValue(value)
                    .build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Clear whitelist addresses",
        form = "clearallowed",
        description = "Clears the list of allowed addresses"
    ) {
        try {
            context.adminService()
                .clearAllowed(VeriBlockMessages.ClearAllowedRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "List whitelist addresses",
        form = "listallowed",
        description = "Returns a list of allowed addresses"
    ) {
        try {
            context.adminService()
                .listAllowed(VeriBlockMessages.ListAllowedRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Remove Whitelist Address",
        form = "removeallowed",
        description = "Remove allowed addresses",
        parameters = listOf(
            CommandParameter(name = "address", type = CommandParameterType.STRING, required = true)
        )
    ) {
        val value: String = getParameter("address")
        try {
            context.adminService()
                .setAllowed(VeriBlockMessages.SetAllowedRequest.newBuilder()
                    .setCommand(VeriBlockMessages.SetAllowedRequest.Command.REMOVE)
                    .setValue(value)
                    .build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }
}

fun VeriBlockMessages.ListAllowedReply.toShellResult(
    context: DefaultCommandContext
): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<ListAllowedPayload>(resultsList)
        temp.success = true
        temp.payload = ListAllowedPayload(this)
        context.outputObject(temp)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

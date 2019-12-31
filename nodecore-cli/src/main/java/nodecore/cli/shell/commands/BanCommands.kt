package nodecore.cli.shell.commands

import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.commands.DefaultCommandContext
import nodecore.cli.commands.serialization.EmptyPayload
import nodecore.cli.commands.serialization.FormattableObject
import nodecore.cli.commands.serialization.ListBannedMinersPayload
import nodecore.cli.commands.serialization.ListBannedPayload
import org.veriblock.shell.Command
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun Shell.banCommands(
    context: DefaultCommandContext
) {
    command(
        name = "Clear banned peers",
        form = "clearbannedpeers",
        description = "Clears the list of peer connections that have been banned"
    ) {
         try {
             context.adminService().clearBanned(VeriBlockMessages.ClearBannedRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
             failure("V800", "Command failure", "Failed to run the command: ${e.message}")
         }
    }

    command(
        name = "Clear banned miners",
        form = "clearpoolbans",
        description = "Clears the list of pool connections that have been banned"
    ) {
        try {
            context.adminService().clearBannedMiners(VeriBlockMessages.ClearBannedMinersRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "List banned peers",
        form = "listbannedpeers",
        description = "Returns a list of peers that are currently banned"
    ) {
        try {
            context.adminService().listBanned(VeriBlockMessages.ListBannedRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "List banned miners",
        form = "listbannedminers",
        description = "Returns a list of UCP clients that are currently banned"
    ) {
        try {
            context.adminService().listBannedMiners(VeriBlockMessages.ListBannedMinersRequest.newBuilder().build())
                .toShellResult(context)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }
}

fun VeriBlockMessages.ProtocolReply.toShellResult(
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
        context.suggestCommands(suggestedCommands)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.ListBannedReply.toShellResult(context: DefaultCommandContext): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<ListBannedPayload>(resultsList)
        temp.success = true
        temp.payload = ListBannedPayload(this)
        context.outputObject(temp)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

fun VeriBlockMessages.ListBannedMinersReply.toShellResult(context: DefaultCommandContext): Result {
    return if (!success) {
        failure {
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    } else {
        val temp = FormattableObject<ListBannedMinersPayload>(resultsList)
        temp.success = true
        temp.payload = ListBannedMinersPayload(this)
        context.outputObject(temp)
        success{
            for (r in resultsList) {
                addMessage(r.code, r.message, r.details, r.error)
            }
        }
    }
}

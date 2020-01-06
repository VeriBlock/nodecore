package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.CliShell
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.ListAllowedPayload
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.CommandParameter

fun CliShell.whitelistCommands() {
    rpcCommand(
        name = "Add Whitelist Address",
        form = "addallowed",
        description = "Add allowed addresses",
        parameters = listOf(
            CommandParameter(name = "address", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val value: String = getParameter("address")
        val request = VeriBlockMessages.SetAllowedRequest.newBuilder()
            .setCommand(VeriBlockMessages.SetAllowedRequest.Command.ADD)
            .setValue(value)
            .build()
        val result = adminService.setAllowed(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Clear whitelist addresses",
        form = "clearallowed",
        description = "Clears the list of allowed addresses"
    ) {
        val request = VeriBlockMessages.ClearAllowedRequest.newBuilder().build()
        val result = adminService.clearAllowed(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "List whitelist addresses",
        form = "listallowed",
        description = "Returns a list of allowed addresses"
    ) {
        val request = VeriBlockMessages.ListAllowedRequest.newBuilder().build()
        val result = adminService.listAllowed(request)

        prepareResult(result.success, result.resultsList) {
            ListAllowedPayload(result)
        }
    }

    rpcCommand(
        name = "Remove Whitelist Address",
        form = "removeallowed",
        description = "Remove allowed addresses",
        parameters = listOf(
            CommandParameter(name = "address", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val value: String = getParameter("address")
        val request = VeriBlockMessages.SetAllowedRequest.newBuilder()
            .setCommand(VeriBlockMessages.SetAllowedRequest.Command.REMOVE)
            .setValue(value)
            .build()
        val result = adminService.setAllowed(request)

        prepareResult(result.success, result.resultsList)
    }
}

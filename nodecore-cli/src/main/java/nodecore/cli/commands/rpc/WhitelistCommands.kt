package nodecore.cli.commands.rpc

import nodecore.api.grpc.RpcClearAllowedRequest
import nodecore.api.grpc.RpcListAllowedRequest
import nodecore.api.grpc.RpcSetAllowedRequest
import nodecore.cli.cliShell
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.ListAllowedPayload
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers

fun CommandFactory.whitelistCommands() {
    rpcCommand(
        name = "Add Whitelist Address",
        form = "addallowed",
        description = "Add allowed addresses",
        parameters = listOf(
            CommandParameter(name = "address", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val value: String = getParameter("address")
        val request = RpcSetAllowedRequest.newBuilder()
            .setCommand(RpcSetAllowedRequest.Command.ADD)
            .setValue(value)
            .build()
        val result = cliShell.adminService.setAllowed(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Clear whitelist addresses",
        form = "clearallowed",
        description = "Clears the list of allowed addresses"
    ) {
        val request = RpcClearAllowedRequest.newBuilder().build()
        val result = cliShell.adminService.clearAllowed(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "List whitelist addresses",
        form = "listallowed",
        description = "Returns a list of allowed addresses"
    ) {
        val request = RpcListAllowedRequest.newBuilder().build()
        val result = cliShell.adminService.listAllowed(request)

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
        val request = RpcSetAllowedRequest.newBuilder()
            .setCommand(RpcSetAllowedRequest.Command.REMOVE)
            .setValue(value)
            .build()
        val result = cliShell.adminService.setAllowed(request)

        prepareResult(result.success, result.resultsList)
    }
}

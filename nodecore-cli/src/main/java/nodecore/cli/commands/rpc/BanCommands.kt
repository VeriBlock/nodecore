package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.cliShell
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.ListBannedMinersPayload
import nodecore.cli.serialization.ListBannedPayload
import org.veriblock.shell.CommandFactory

fun CommandFactory.banCommands() {
    rpcCommand(
        name = "Clear banned peers",
        form = "clearbannedpeers",
        description = "Clears the list of peer connections that have been banned"
    ) {
        val request = VeriBlockMessages.ClearBannedRequest.newBuilder().build()
        val result = cliShell.adminService.clearBanned(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "Clear banned miners",
        form = "clearpoolbans",
        description = "Clears the list of pool connections that have been banned"
    ) {
        val request = VeriBlockMessages.ClearBannedMinersRequest.newBuilder().build()
        val result = cliShell.adminService.clearBannedMiners(request)

        prepareResult(result.success, result.resultsList)
    }

    rpcCommand(
        name = "List banned peers",
        form = "listbannedpeers",
        description = "Returns a list of peers that are currently banned"
    ) {
        val request = VeriBlockMessages.ListBannedRequest.newBuilder().build()
        val result = cliShell.adminService.listBanned(request)

        prepareResult(result.success, result.resultsList) {
            ListBannedPayload(result)
        }
    }

    rpcCommand(
        name = "List banned miners",
        form = "listbannedminers",
        description = "Returns a list of UCP clients that are currently banned"
    ) {
        val request = VeriBlockMessages.ListBannedMinersRequest.newBuilder().build()
        val result = cliShell.adminService.listBannedMiners(request)

        prepareResult(result.success, result.resultsList) {
            ListBannedMinersPayload(result)
        }
    }
}

package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import nodecore.cli.serialization.AddressHistoryInfo
import nodecore.cli.serialization.GetDiagnosticInfoPayload
import nodecore.cli.serialization.GetInfoPayload
import nodecore.cli.serialization.GetStateInfoPayload
import nodecore.cli.serialization.PeerInfoPayload
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter

fun CommandFactory.infoCommands() {
    rpcCommand(
        name = "Get diagnostic Info",
        form = "getdiagnosticinfo",
        description = "Returns diagnostic info about the NodeCore instance"
    ) {
        val request = VeriBlockMessages.GetDiagnosticInfoRequest.newBuilder().build()
        val result = cliShell.adminService.getDiagnosticInfo(request)

        prepareResult(result.success, result.resultsList) {
            GetDiagnosticInfoPayload(result)
        }
    }

    rpcCommand(
        name = "Get Info",
        form = "getinfo|info|inf|i",
        description = "Returns information about the node and the current blockchain",
        suggestedCommands = { listOf("getbalance", "gettransaction", "getblockfromindex", "getblockfromhash", "getstateinfo") }
    ) {
        val request = VeriBlockMessages.GetInfoRequest.newBuilder().build()
        val result = cliShell.adminService.getInfo(request)

        prepareResult(result.success, result.resultsList) {
            GetInfoPayload(result)
        }
    }

    rpcCommand(
        name = "Get Peer Info",
        form = "getpeerinfo",
        description = "Returns a list of connected peers",
        suggestedCommands = { listOf("addnode", "removenode") }
    ) {
        val request = VeriBlockMessages.GetPeerInfoRequest.newBuilder().build()
        val result = cliShell.adminService.getPeerInfo(request)

        prepareResult(result.success, result.resultsList) {
            PeerInfoPayload(result)
        }
    }

    rpcCommand(
        name = "Get state Info",
        form = "getstateinfo|stateinfo|state|getstate",
        description = "Returns blockchain, operating, and network state information",
        suggestedCommands = { listOf("getinfo") }
    ) {
        val request = VeriBlockMessages.GetStateInfoRequest.newBuilder().build()
        val result = cliShell.adminService.getStateInfo(request)

        prepareResult(result.success, result.resultsList) {
            GetStateInfoPayload(result)
        }
    }

    rpcCommand(
        name = "Get History",
        form = "gethistory",
        description = "Gets transaction history for an address",
        parameters = listOf(
            CommandParameter(name = "address", mapper = ShellCommandParameterMappers.STANDARD_ADDRESS, required = false)
        ),
        suggestedCommands = { listOf("getbalance", "gettransaction") }
    ) {
        val address: String? = getOptionalParameter("address")
        val request = VeriBlockMessages.GetHistoryRequest.newBuilder()
        if (address != null) {
            request.addAddresses(ByteStringAddressUtility.createProperByteStringAutomatically(address))
        }
        val result = cliShell.adminService.getHistory(request.build())

        prepareResult(result.success, result.resultsList) {
            result.addressesList.map { AddressHistoryInfo(it) }
        }
    }
}

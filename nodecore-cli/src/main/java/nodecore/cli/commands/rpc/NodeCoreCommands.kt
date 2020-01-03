package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.CliShell
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand

fun CliShell.nodeCoreCommands() {
    rpcCommand(
        name = "Stop NodeCore",
        form = "stopnodecore",
        description = "Stop NodeCore"
    ) {
        val request = VeriBlockMessages.StopNodeCoreRequest.newBuilder().build()
        val result = adminService.stopNodeCore(request)

        prepareResult(result.success, result.resultsList)
    }
}

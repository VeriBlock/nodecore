package nodecore.cli.commands.rpc

import nodecore.api.grpc.RpcStopNodeCoreRequest
import nodecore.cli.cliShell
import nodecore.cli.prepareResult
import nodecore.cli.rpcCommand
import org.veriblock.shell.CommandFactory

fun CommandFactory.nodeCoreCommands() {
    rpcCommand(
        name = "Stop NodeCore",
        form = "stopnodecore",
        description = "Stop NodeCore"
    ) {
        val request = RpcStopNodeCoreRequest.newBuilder().build()
        val result = cliShell.adminService.stopNodeCore(request)

        prepareResult(result.success, result.resultsList)
    }
}

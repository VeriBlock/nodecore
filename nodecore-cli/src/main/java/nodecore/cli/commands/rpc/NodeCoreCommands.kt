package nodecore.cli.commands.rpc

import nodecore.api.grpc.VeriBlockMessages
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
        val request = VeriBlockMessages.StopNodeCoreRequest.newBuilder().build()
        val result = cliShell.adminService.stopNodeCore(request)

        prepareResult(result.success, result.resultsList)
    }
}

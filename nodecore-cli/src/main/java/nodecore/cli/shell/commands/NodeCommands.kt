package nodecore.cli.shell.commands

import io.grpc.StatusRuntimeException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.cli.commands.DefaultCommandContext
import nodecore.cli.contracts.PeerEndpoint
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.failure

fun Shell.nodeCommands(
    context: DefaultCommandContext
) {
    command(
        name = "Add Node",
        form = "addnode",
        description = "Add a peer node to the local configuration and connect",
        parameters = listOf(
            CommandParameter(name = "peer", type = CommandParameterType.PEER, required = true)
        )
    ) {
        val peer: PeerEndpoint = getParameter("peer")
        try {
            val endpoint = VeriBlockMessages.Endpoint.newBuilder().setAddress(peer.address())
                .setPort(peer.port().toInt()).build()
            val suggestedCommands = listOf(getCommand("removenode"))

            context.adminService().addNode(VeriBlockMessages.NodeRequest.newBuilder().addEndpoint(endpoint).build())
                .toShellResult(context, suggestedCommands)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }

    command(
        name = "Remove Node",
        form = "removenode",
        description = "Removes the peer address from the configuration",
        parameters = listOf(
            CommandParameter(name = "peer", type = CommandParameterType.PEER, required = true)
        )
    ) {
        val peer: PeerEndpoint = getParameter("peer")
        try {
            val endpoint = VeriBlockMessages.Endpoint.newBuilder().setAddress(peer.address())
                .setPort(peer.port().toInt()).build()
            val suggestedCommands = listOf(getCommand("addnode"))

            context.adminService().removeNode(VeriBlockMessages.NodeRequest.newBuilder().addEndpoint(endpoint).build())
                .toShellResult(context, suggestedCommands)
        } catch (e: StatusRuntimeException) {
            failure("V800", "Command failure", "Failed to run the command: ${e.message}")
        }
    }
}

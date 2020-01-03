package nodecore.cli.commands.shell

import nodecore.cli.CliShell
import nodecore.cli.command
import nodecore.cli.contracts.PeerEndpoint
import nodecore.cli.contracts.ProtocolEndpoint
import nodecore.cli.contracts.ProtocolEndpointType
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.core.success
import java.awt.GraphicsEnvironment

fun CliShell.connectionCommands() {
    command(
        name = "Connect",
        form = "connect",
        description = "Connect to a NodeCore RPC endpoint. Note: NodeCore does not begin listening for RPC " +
            "connections until after loading the blockchain, which may take several minutes. By default, " +
            "NodeCore MainNet listens on port 10500 and NodeCore TestNet listens on port 10501.",
        parameters = listOf(
            CommandParameter(name = "peer", type = CommandParameterType.PEER, required = true),
            CommandParameter(name = "password", type = CommandParameterType.STRING, required = false)
        ),
        suggestedCommands = {
            if (GraphicsEnvironment.isHeadless()) {
                listOf("getnewaddress", "getbalance", "startsolopool")
            } else {
                listOf("getnewaddress", "getbalance", "startsolopool", "startpopminer")
            }
        }
    ) {
        val type = ProtocolEndpointType.RPC
        val peer: PeerEndpoint = getParameter("peer")
        val passwordParam: String? = getOptionalParameter("password")
        val endpoint = ProtocolEndpoint(peer.toString(), type, passwordParam)

        this.extraData["connect"] = endpoint

        success()
    }

    command(
        name = "Disconnect",
        form = "disconnect",
        description = "Disconnect from the open P2P or RPC connection"
    ) {
        this.extraData["disconnect"] = true

        success()
    }
}

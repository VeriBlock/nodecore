package nodecore.cli.commands.shell

import nodecore.cli.cliCommand
import nodecore.cli.cliShell
import nodecore.cli.commands.ShellCommandParameterMappers
import nodecore.cli.contracts.PeerEndpoint
import nodecore.cli.contracts.ProtocolEndpoint
import nodecore.cli.contracts.ProtocolEndpointType
import nodecore.cli.models.ModeType
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.core.success
import veriblock.conf.NetworkParameters
import veriblock.net.BootstrapPeerDiscovery
import veriblock.net.LocalhostDiscovery
import veriblock.net.PeerDiscovery
import java.awt.GraphicsEnvironment

fun CommandFactory.connectionCommands() {
    cliCommand(
        name = "Connect",
        form = "connect",
        description = "Connect to a NodeCore RPC endpoint. Note: NodeCore does not begin listening for RPC " +
            "connections until after loading the blockchain, which may take several minutes. By default, " +
            "NodeCore MainNet listens on port 10500 and NodeCore TestNet listens on port 10501.",
        parameters = listOf(
            CommandParameter(name = "peer", mapper = ShellCommandParameterMappers.PEER, required = true),
            CommandParameter(name = "password", mapper = CommandParameterMappers.STRING, required = false)
        ),
        suggestedCommands = {
            if (GraphicsEnvironment.isHeadless()) {
                listOf("getinfo", "getnewaddress", "getbalance", "startsolopool")
            } else {
                listOf("getinfo", "getnewaddress", "getbalance", "startsolopool", "startpopminer")
            }
        }
    ) {
        val shell = cliShell
        val type = ProtocolEndpointType.RPC
        val peer: PeerEndpoint = getParameter("peer")
        val passwordParam: String? = getOptionalParameter("password")
        val endpoint = ProtocolEndpoint(peer.toString(), type, passwordParam)

        this.extraData["connect"] = endpoint
        shell.modeType = ModeType.STANDARD

        success()
    }


    cliCommand(
        name = "Connect SPV",
        form = "startspv",
        description = "Connect to a NodeCore RPC endpoint in the spv mode. Note: NodeCore does not begin listening for RPC " +
            "connections until after loading the blockchain, which may take several minutes.",
        parameters = listOf(
            CommandParameter(name = "mainnet/testnet/alphanet", mapper = ShellCommandParameterMappers.NET, required = true),
            CommandParameter(name = "peer", mapper = CommandParameterMappers.STRING, required = false)
        ),
        suggestedCommands = {
            listOf("getbalance", "send")
        }
    ) {
        val shell = cliShell
        val net: NetworkParameters = getParameter("mainnet/testnet/alphanet")
        val peer: String? = getOptionalParameter("peer")

        // Work with bootstraps peers by default.
        val peerDiscovery: PeerDiscovery = if (peer == "local") LocalhostDiscovery(net) else BootstrapPeerDiscovery(net)

        val endpoint = shell.startSPV(net, peerDiscovery)

        this.extraData["connect"] = endpoint
        this.extraData["disconnectCallBack"] = Runnable {
            shell.spvContext.shutdown()
        }

        success()
    }

    cliCommand(
        name = "Disconnect",
        form = "disconnect",
        description = "Disconnect from the open P2P or RPC connection"
    ) {
        val shell = cliShell
        this.extraData["disconnect"] = true
        shell.modeType = ModeType.STANDARD

        success()
    }
}

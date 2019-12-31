// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.shell;

import nodecore.cli.annotations.CommandParameterType;
import nodecore.cli.annotations.CommandServiceType;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandContext;
import nodecore.cli.contracts.PeerEndpoint;
import nodecore.cli.contracts.ProtocolEndpoint;
import nodecore.cli.contracts.ProtocolEndpointType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

@CommandSpec(
        name = "Connect",
        form = "connect",
        requiresConnection = false,
        service = CommandServiceType.SHELL,
        description = "Connect to a NodeCore RPC endpoint. Note: NodeCore does not begin listening for RPC " +
                "connections until after loading the blockchain, which may take several minutes. By default, " +
                "NodeCore MainNet listens on port 10500 and NodeCore TestNet listens on port 10501.")
@CommandSpecParameter(name = "peer", required = true, type = CommandParameterType.PEER)
@CommandSpecParameter(name = "password", required = false, type = CommandParameterType.STRING)
public class ConnectCommand implements Command {
    private static final Logger _logger = LoggerFactory.getLogger(ConnectCommand.class);

    public ConnectCommand() {
        // TODO: ADD THESE SUGGESTED COMMANDS WHEN REFACTORING:
        List<Class<? extends Command>> suggestedCommands = new ArrayList<>(Arrays.asList(GetInfoCommand.class,
            GetNewAddressCommand.class,
            GetBalanceCommand.class,
            StartSoloPoolCommand.class));

        // TODO: GOOD LUCK IMPLEMENTING THIS
        if (!GraphicsEnvironment.isHeadless()) {
            suggestedCommands.add(StartPoPMinerCommand.class);
        }
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        ProtocolEndpointType type = ProtocolEndpointType.RPC;

        PeerEndpoint peer = context.getParameter("peer");
        String passwordParam = context.getParameter("password");

        ProtocolEndpoint endpoint = new ProtocolEndpoint(peer.toString(), type, passwordParam);

        context.putData("connect", endpoint);

        return result;
    }

}

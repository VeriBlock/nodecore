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
import nodecore.cli.contracts.CommandDefinition;
import nodecore.cli.contracts.CommandFactory;
import nodecore.cli.contracts.ProtocolEndpointType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandSpec(
        name = "Help",
        form = "help",
        requiresConnection = false,
        service = CommandServiceType.SHELL,
        description = "Returns this help message")
@CommandSpecParameter(name = "command", required = false, type = CommandParameterType.STRING)
public class HelpCommand implements Command {
    private CommandFactory _factory;

    public HelpCommand(CommandFactory factory) {
        _factory = factory;
    }

    private boolean isValidType(CommandServiceType serviceType, ProtocolEndpointType protocolType) {
        return (serviceType == CommandServiceType.PEER && protocolType == ProtocolEndpointType.PEER)
                || (serviceType == CommandServiceType.RPC && protocolType == ProtocolEndpointType.RPC)
                || serviceType == CommandServiceType.SHELL;
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        String command = context.getParameter("command");

        if (command == null) {
            Map<CommandServiceType, List<CommandDefinition>> categories = new HashMap<>();
            for (String key : _factory.getDefinitions().keySet()) {
                CommandDefinition def = _factory.getDefinitions().get(key);
                if (!isValidType(def.getSpec().service(), context.getProtocolType()))
                    continue;

                if (def.getSpec().requiresConnection() && !context.isConnected())
                    continue;

                List<CommandDefinition> list = categories.computeIfAbsent(
                        def.getSpec().service(),
                        k -> new ArrayList<>());
                list.add(def);
                list.sort(Comparator.comparing(a -> a.getSpec().form()));
            }

            context.write().normal("Commands:\n");
            for (CommandServiceType category : categories.keySet()) {
                context.write().inverted(String.format("\n %s: \n", category.name()));
                List<CommandDefinition> list = categories.get(category);
                for (CommandDefinition def : list) {
                    context.write().normal(String.format("    %s", def.getSpec().form()));
                    formatParameters(context, def.getParams());
                    context.write().normal("\n");
                }

            }
            context.write().normal("\n");
            context.write().normal("    All RPC Commands support following selectors:\n");
            context.write().normal("        -o <filename>       Saves command output into a file\n");
            context.write().normal("        Example: getinfo -o abcde.json");
            context.write().normal("\n");
        } else {
            CommandDefinition def = _factory.getDefinitions().get(command);
            if (def != null
            && isValidType(def.getSpec().service(), context.getProtocolType())
            && (!def.getSpec().requiresConnection() || (def.getSpec().requiresConnection() && context.isConnected()))) {
                context.write().normal(String.format("\nCommand: %s\n", def.getSpec().name()));
                context.write().normal(String.format("\n%s", def.getSpec().form()));
                formatParameters(context, def.getParams());
                context.write().normal(String.format("\n%s\n\n", def.getSpec().description()));
            } else {
                result.addMessage(
                        "V004",
                        "Unknown protocol command",
                        String.format("The command '%s' is unknown. Type 'help' to view all commands.", command),
                        true);
                result.fail();
            }
        }

        return result;
    }

    private void formatParameters(CommandContext context, CommandSpecParameter[] params) {
        if (params.length > 0) {
            context.write().normal(" ");
            for (CommandSpecParameter param : params) {
                if (param.required()) {
                    context.write().normal(String.format("<%s> ", param.name()));
                } else {
                    context.write().normal(String.format("[%s] ", param.name()));
                }
            }
        }
    }
}

// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandParameterType;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import nodecore.miners.pop.shell.annotations.CommandSpecParameter;

import java.util.*;

@CommandSpec(
        name = "Help",
        form = "help",
        description = "Returns this help message")
@CommandSpecParameter(name = "command", required = false, type = CommandParameterType.STRING)
public class HelpCommand implements Command {
    private CommandFactory _factory;

    @Inject
    public HelpCommand(CommandFactory factory) {
        _factory = factory;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();

        String command = context.getParameter("command");

        if (command == null) {
            Map<String, CommandDefinition> definitions = new TreeMap<>(_factory.getDefinitions());

            context.writeToOutput("Commands:");
            for (String alias : definitions.keySet()) {
                context.writeToOutput("    %s %s", alias, formatParameters(definitions.get(alias).getParams()));
            }
        } else {
            CommandDefinition def = _factory.getDefinitions().get(command);
            if (def != null) {
                context.writeToOutput("Command: %s", def.getSpec().name());
                context.writeToOutput("%s %s", def.getSpec().form(), formatParameters(def.getParams()));
                context.writeToOutput("%s", def.getSpec().description());
            } else {
                result.addMessage(
                        "V004",
                        "Unknown protocol command",
                        String.format("The command '%s' is not supported", command),
                        true);
                result.fail();
            }
        }
        context.flush();

        return result;
    }

    private String formatParameters(CommandSpecParameter[] params) {
        StringBuilder stringBuilder = new StringBuilder();
        if (params.length > 0) {
            for (CommandSpecParameter param : params) {
                if (param.required()) {
                    stringBuilder.append(String.format("<%s> ", param.name()));
                } else {
                    stringBuilder.append(String.format("[%s] ", param.name()));
                }
            }
        }
        return stringBuilder.toString();
    }
}

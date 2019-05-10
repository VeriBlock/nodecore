// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.contracts.Configuration;
import nodecore.miners.pop.shell.annotations.CommandParameterType;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import nodecore.miners.pop.shell.annotations.CommandSpecParameter;
import nodecore.miners.pop.contracts.Command;
import nodecore.miners.pop.contracts.CommandContext;
import nodecore.miners.pop.contracts.Result;

@CommandSpec(
        name = "Set Config",
        form = "setconfig",
        description = "Sets a new value for a config property")
@CommandSpecParameter(name = "key", required = true, type = CommandParameterType.STRING)
@CommandSpecParameter(name = "value", required = true, type = CommandParameterType.STRING)
public class SetConfigCommand implements Command {
    private final Configuration configuration;

    @Inject
    public SetConfigCommand(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        String key = context.getParameter("key");
        String value = context.getParameter("value");

        return configuration.setProperty(key, value);
    }
}

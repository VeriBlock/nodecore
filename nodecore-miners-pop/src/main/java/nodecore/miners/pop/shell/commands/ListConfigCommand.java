// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandSpec;

@CommandSpec(
        name = "List Config",
        form = "listconfig",
        description = "Lists the current configuration properties and values")
public class ListConfigCommand implements Command {
    private final Configuration configuration;

    @Inject
    public ListConfigCommand(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        context.writeToOutput("Configuration Properties:");

        configuration.list().forEach(s -> context.writeToOutput("    %s", s));

        context.flush();

        return new DefaultResult();
    }
}

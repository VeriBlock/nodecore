// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import nodecore.miners.pop.contracts.Command;
import nodecore.miners.pop.contracts.CommandContext;
import nodecore.miners.pop.contracts.DefaultResult;
import nodecore.miners.pop.contracts.Result;
import nodecore.miners.pop.shell.annotations.CommandSpec;

@CommandSpec(
        name = "Quit",
        form = "quit|exit",
        description = "Quit the application")
public class QuitCommand implements Command {
    @Inject
    public QuitCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        context.quit();
        return new DefaultResult();
    }

}

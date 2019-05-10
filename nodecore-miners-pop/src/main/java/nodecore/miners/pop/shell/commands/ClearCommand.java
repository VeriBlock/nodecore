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
        name = "Clear Screen",
        form = "clear",
        description = "Clears the terminal screen")
public class ClearCommand implements Command {
    @Inject
    public ClearCommand() {
    }

    @Override
    public Result execute(CommandContext context) {
        context.clear();
        return new DefaultResult();
    }
}

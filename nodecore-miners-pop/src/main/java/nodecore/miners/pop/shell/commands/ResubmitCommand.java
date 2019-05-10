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
import nodecore.miners.pop.contracts.PoPMiner;
import nodecore.miners.pop.contracts.Result;
import nodecore.miners.pop.shell.annotations.CommandParameterType;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import nodecore.miners.pop.shell.annotations.CommandSpecParameter;

@CommandSpec(
        name = "Resubmit Operation",
        form = "resubmit",
        description = "Resubmits an operation")
@CommandSpecParameter(name = "id", required = true, type = CommandParameterType.STRING)
public class ResubmitCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ResubmitCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        String id = context.getParameter("id");

        return popMiner.resubmit(id);
    }
}

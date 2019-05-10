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

import java.util.List;

@CommandSpec(
        name = "List Operations",
        form = "listoperations",
        description = "Lists the current running operations")
public class ListOperationsCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ListOperationsCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        List<OperationSummary> operations = popMiner.listOperations();

        if (operations.size() > 0) {
            context.writeToOutput("Running operations:");
            for (OperationSummary summary : operations) {
                context.writeToOutput("    '%s': { state: '%s', action: '%s', endorsed_block: %d }",
                        summary.getOperationId(), summary.getState(), summary.getAction(), summary.getEndorsedBlockNumber());
                if (summary.getMessage() != null && summary.getMessage().length() > 0) {
                    context.writeToOutput("                %s", summary.getMessage());
                }
            }
        } else {
            context.writeToOutput("No running operations");
        }
        context.flush();

        return new DefaultResult();
    }
}

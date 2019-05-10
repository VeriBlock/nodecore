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
import org.apache.commons.lang3.tuple.Pair;

@CommandSpec(
        name = "Show Recent Bitcoin Fees",
        form = "showrecentbitcoinfees",
        description = "Returns the average fee per byte in a recent Bitcoin block")
public class ShowRecentBitcoinFeesCommand implements Command {
    private final PoPMiner popMiner;

    @Inject
    public ShowRecentBitcoinFeesCommand(PoPMiner popMiner) {
        this.popMiner = popMiner;
    }

    @Override
    public Result execute(CommandContext context) throws Exception {
        Result result = new DefaultResult();
        try {
            Pair<Integer, Long> blockFees = popMiner.showRecentBitcoinFees();
            if (blockFees == null) {
                result.fail();
                result.addMessage("V500", "Error", "Unable to fetch fees for recent block", true);
            } else {
                context.writeToOutput("Bitcoin Block #%d -> Average Fee per Byte: %d", blockFees.getLeft(), blockFees.getRight());
                context.flush();
            }
        } catch (Exception e) {
            result.fail();
            result.addMessage("V500", "Error", e.getMessage(), true);
        }

        return result;
    }
}

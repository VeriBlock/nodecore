// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell.commands;

import com.google.inject.Inject;
import io.grpc.StatusRuntimeException;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.shell.annotations.CommandSpec;

import java.util.Comparator;
import java.util.List;

@CommandSpec(
        name = "View Recent Rewards",
        form = "viewrecentrewards",
        description = "Lists recent and upcoming rewards")
public class ViewRecentRewardsCommand implements Command {
    private final NodeCoreService popService;

    @Inject
    public ViewRecentRewardsCommand(NodeCoreService popService) {
        this.popService = popService;
    }

    @Override
    public Result execute(CommandContext context) {
        Result result = new DefaultResult();

        try {
            List<PoPEndorsementInfo> endorsements = popService.getPoPEndorsementInfo();
            endorsements.sort(Comparator.comparingInt(p -> p.endorsedBlockNumber));

            for (PoPEndorsementInfo e : endorsements) {
                context.writeToOutput("{endorsed_block: %d, %s: %s, paid_in_block: %d}",
                        e.endorsedBlockNumber, e.finalized ? "reward" : "projected_reward", e.reward, e.endorsedBlockNumber + 500);
                context.flush();
            }
        } catch (StatusRuntimeException e) {
            result.fail();
            result.addMessage("V500", "NodeCore Communication Error", e.getStatus().getCode().toString(), true);
        } catch (Exception e) {
            result.fail();
            result.addMessage("V500", "Command Error", e.getMessage(), true);
        }

        return result;
    }
}

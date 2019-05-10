// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules.actions;

import com.google.inject.Inject;
import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.contracts.MineResult;
import nodecore.miners.pop.contracts.PoPMiner;
import nodecore.miners.pop.contracts.ResultMessage;
import nodecore.miners.pop.events.ErrorMessageEvent;
import org.apache.commons.lang3.StringUtils;

public class MineAction implements RuleAction<Integer> {
    private final PoPMiner miner;

    @Inject
    public MineAction(PoPMiner miner) {
        this.miner = miner;
    }

    @Override
    public void execute(Integer blockHeight) {
        MineResult result = miner.mine(blockHeight);
        if (result.didFail()) {
            StringBuilder errorMessage = new StringBuilder();
            for (ResultMessage message : result.getMessages()) {
                errorMessage.append(System.lineSeparator())
                        .append(message.getMessage())
                        .append(": ")
                        .append(StringUtils.join(message.getDetails(), "; "));
            }
            InternalEventBus.getInstance().post(new ErrorMessageEvent(String.format("Mine Action Failed: %s", errorMessage.toString())));
        }
    }
}

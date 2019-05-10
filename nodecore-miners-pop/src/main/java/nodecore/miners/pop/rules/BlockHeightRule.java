// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules;

import com.google.inject.Inject;
import nodecore.miners.pop.contracts.Configuration;
import nodecore.miners.pop.contracts.VeriBlockHeader;
import nodecore.miners.pop.rules.actions.Mining;
import nodecore.miners.pop.rules.actions.RuleAction;
import nodecore.miners.pop.rules.conditions.*;

import java.util.ArrayList;
import java.util.List;

public class BlockHeightRule implements Rule {
    private final Configuration configuration;

    private List<Condition<Integer>> conditions = new ArrayList<>();
    public List<Condition<Integer>> getConditions() {
        return conditions;
    }
    public void setConditions(List<Condition<Integer>> conditions) {
        this.conditions = conditions;
    }

    private RuleAction<Integer> action;
    public RuleAction<Integer> getAction() {
        return action;
    }
    public void setAction(RuleAction<Integer> action) {
        this.action = action;
    }

    @Inject
    public BlockHeightRule(@Mining RuleAction<Integer> action, Configuration configuration) {
        this.action = action;
        this.configuration = configuration;

        this.conditions.add(new KeystoneBlockCondition());
        this.conditions.add(new Round1Condition());
        this.conditions.add(new Round2Condition());
        this.conditions.add(new Round3Condition());
    }

    @Override
    public void evaluate(RuleContext context) {
        VeriBlockHeader previousHead = context.getPreviousHead();
        VeriBlockHeader latestBlock = context.getLatestBlock();

        int start, end;
        start = end = latestBlock.getHeight();
        if (previousHead != null) {
            start = previousHead.getHeight() + 1;
        }

        for (int i = start; i <= end; i++) {
            for (Condition<Integer> condition : conditions) {
                if (condition.isActive(configuration) && condition.evaluate(i)) {
                    action.execute(i);
                }
            }
        }

    }
}

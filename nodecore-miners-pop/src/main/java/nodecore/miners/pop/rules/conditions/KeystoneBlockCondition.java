// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules.conditions;

import nodecore.miners.pop.contracts.Configuration;

public class KeystoneBlockCondition implements Condition<Integer> {
    @Override
    public boolean isActive(Configuration configuration) {
        return configuration.getBoolean("auto.mine.round4");
    }

    @Override
    public boolean evaluate(Integer subject) {
        return subject != null && (subject % 20) == 0;
    }
}

// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules.conditions;

import nodecore.miners.pop.contracts.Configuration;

public interface Condition<T> {
    boolean isActive(Configuration configuration);

    boolean evaluate(T subject);
}

// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.rules.conditions

import nodecore.miners.pop.Configuration

class Round2Condition : Condition<Int> {
    override fun isActive(configuration: Configuration): Boolean {
        return configuration.getBoolean("auto.mine.round2")
    }

    override fun evaluate(subject: Int?): Boolean {
        return subject != null && subject % 20 != 0 && subject % 20 % 3 == 2
    }
}

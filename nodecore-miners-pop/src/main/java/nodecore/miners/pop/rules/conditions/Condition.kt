// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.rules.conditions

import nodecore.miners.pop.OldConfiguration
import nodecore.miners.pop.VpmConfig

interface Condition<T> {
    fun isActive(configuration: VpmConfig): Boolean
    fun evaluate(subject: T?): Boolean
}

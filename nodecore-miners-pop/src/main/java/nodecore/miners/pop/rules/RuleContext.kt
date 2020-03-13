// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.rules

import nodecore.miners.pop.model.VeriBlockHeader

class RuleContext(
    var previousHead: VeriBlockHeader?,
    var latestBlock: VeriBlockHeader?
) {
    var blocksRemoved: List<VeriBlockHeader>? = null
    var blocksAdded: List<VeriBlockHeader>? = null
}

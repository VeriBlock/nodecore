// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.sdk.models.FullBlock
import org.veriblock.sdk.models.VeriBlockBlock

class BlockChainReorganizedEventData(
    val oldBlocks: List<VeriBlockBlock>,
    val newBlocks: List<FullBlock>
)
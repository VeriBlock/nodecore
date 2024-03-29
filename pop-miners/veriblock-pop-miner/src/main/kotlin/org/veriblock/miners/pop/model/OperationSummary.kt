// VeriBlock PoP Miner
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

import java.time.LocalDateTime

class OperationSummary(
    val operationId: String,
    val endorsedBlockNumber: Int,
    val status: String,
    val action: String,
    val createdAt: LocalDateTime
) 

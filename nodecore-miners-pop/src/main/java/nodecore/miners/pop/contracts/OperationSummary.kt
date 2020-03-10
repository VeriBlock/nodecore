// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts

class OperationSummary(
    val operationId: String,
    val endorsedBlockNumber: Int,
    val state: String,
    val action: String,
    val message: String
) 

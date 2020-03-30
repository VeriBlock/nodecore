// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import org.veriblock.lite.core.Balance
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.shell.core.Result

class MinerConfig(
    val feePerByte: Long = 1_000,
    val maxFee: Long = 10_000_000,
    val mock: Boolean = false
)

interface Miner {
    fun initialize()

    fun start()

    fun getOperations(): List<MiningOperation>

    fun getOperation(id: String): MiningOperation?

    fun getAddress(): String

    fun getBalance(): Balance?

    fun mine(chainId: String, block: Int?): Result

    fun resubmit(operation: MiningOperation)

    fun shutdown()

    fun setIsShuttingDown(b: Boolean)

    val feePerByte: Long
    val maxFee: Long
}

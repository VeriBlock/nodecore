// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.contracts

import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.abs
import org.veriblock.sdk.models.minus
import org.veriblock.sdk.models.plus

class Balance(
    var confirmedBalance: Coin = Coin.ZERO,
    var pendingBalanceChanges: Coin = Coin.ZERO
) {
    val pendingBalance: Coin
        get() = confirmedBalance + pendingBalanceChanges

    fun addPendingSpend(amount: Coin) {
        pendingBalanceChanges -= amount.abs()
    }

    fun addPendingReceipt(amount: Coin) {
        pendingBalanceChanges += amount.abs()
    }

    fun confirmPendingSpend(amount: Coin) {
        val absAmount = amount.abs()
        pendingBalanceChanges += absAmount
        confirmedBalance -= absAmount
    }

    fun confirmPendingReceipt(amount: Coin) {
        val absAmount = amount.abs()
        pendingBalanceChanges -= absAmount
        confirmedBalance += absAmount
    }

    fun addConfirmedSpend(amount: Coin) {
        confirmedBalance -= amount.abs()
    }

    fun addConfirmedReceipt(amount: Coin) {
        confirmedBalance += amount.abs()
    }

    fun makeConfirmedSpendPending(amount: Coin) {
        val absAmount = amount.abs()
        pendingBalanceChanges -= absAmount
        confirmedBalance += absAmount
    }

    fun makeConfirmedReceiptPending(amount: Coin) {
        val absAmount = amount.abs()
        pendingBalanceChanges += absAmount
        confirmedBalance -= absAmount
    }
}

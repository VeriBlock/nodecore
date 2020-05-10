// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.sdk.models.Coin

class Balance @JvmOverloads constructor(
    var confirmedBalance: Coin,
    var pendingBalanceChanges: Coin = Coin.ZERO
) {
    val pendingBalance: Coin
        get() = confirmedBalance.add(pendingBalanceChanges)

    fun addPendingSpend(amount: Coin) {
        var amount = amount
        if (amount < Coin.ZERO) {
            amount = amount.negate()
        }
        pendingBalanceChanges = pendingBalanceChanges.subtract(amount)
    }

    fun addPendingReceipt(amount: Coin) {
        var amount = amount
        if (amount < Coin.ZERO) {
            amount = amount.negate()
        }
        pendingBalanceChanges = pendingBalanceChanges.add(amount)
    }

    fun confirmPendingSpend(amount: Coin) {
        var amount = amount
        if (amount < Coin.ZERO) {
            amount = amount.negate()
        }
        pendingBalanceChanges = pendingBalanceChanges.add(amount)
        confirmedBalance = confirmedBalance.subtract(amount)
    }

    fun confirmPendingReceipt(amount: Coin) {
        var amount = amount
        if (amount < Coin.ZERO) {
            amount = amount.negate()
        }
        pendingBalanceChanges = pendingBalanceChanges.subtract(amount)
        confirmedBalance = confirmedBalance.add(amount)
    }

    fun addConfirmedSpend(amount: Coin) {
        var amount = amount
        if (amount < Coin.ZERO) {
            amount = amount.negate()
        }
        confirmedBalance = confirmedBalance.subtract(amount)
    }

    fun addConfirmedReceipt(amount: Coin) {
        var amount = amount
        if (amount < Coin.ZERO) {
            amount = amount.negate()
        }
        confirmedBalance = confirmedBalance.add(amount)
    }

    fun makeConfirmedSpendPending(amount: Coin) {
        var amount = amount
        if (amount < Coin.ZERO) {
            amount = amount.negate()
        }
        pendingBalanceChanges = pendingBalanceChanges.subtract(amount)
        confirmedBalance = confirmedBalance.add(amount)
    }

    fun makeConfirmedReceiptPending(amount: Coin) {
        var amount = amount
        if (amount < Coin.ZERO) {
            amount = amount.negate()
        }
        pendingBalanceChanges = pendingBalanceChanges.add(amount)
        confirmedBalance = confirmedBalance.subtract(amount)
    }

    fun clone(): Balance {
        return Balance(confirmedBalance, pendingBalanceChanges)
    }

    companion object {
        fun create(): Balance {
            return Balance(Coin.ZERO, Coin.ZERO)
        }
    }

}

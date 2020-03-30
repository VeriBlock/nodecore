// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.veriblock.sdk.models.Coin

class Balance(
    var confirmedBalance: Coin,
    pendingBalanceChanges: Coin = Coin.ZERO
) {
    var pendingBalanceChanges: Coin = pendingBalanceChanges
        private set

    fun getPendingBalance(): Coin =
        confirmedBalance + pendingBalanceChanges

    fun addPendingSpend(amount: Coin) {
        val absAmount = amount.abs()
        pendingBalanceChanges -= absAmount
    }

    fun addPendingReceipt(amount: Coin) {
        val absAmount = amount.abs()
        pendingBalanceChanges += absAmount
    }

    fun confirmPendingSpend(amount: Coin) {
        val absAmount = amount.abs()
        this.pendingBalanceChanges += absAmount
        this.confirmedBalance -= absAmount
    }

    fun confirmPendingReceipt(amount: Coin) {
        val absAmount = amount.abs()
        this.pendingBalanceChanges -= absAmount
        this.confirmedBalance += absAmount
    }

    fun addConfirmedSpend(amount: Coin) {
        val absAmount = amount.abs()
        this.confirmedBalance -= absAmount
    }

    fun addConfirmedReceipt(amount: Coin) {
        val absAmount = amount.abs()
        this.confirmedBalance += absAmount
    }

    fun makeConfirmedSpendPending(amount: Coin) {
        val absAmount = amount.abs()
        this.pendingBalanceChanges -= absAmount
        this.confirmedBalance += amount
    }

    fun makeConfirmedReceiptPending(amount: Coin) {
        val absAmount = amount.abs()
        this.pendingBalanceChanges += absAmount
        this.confirmedBalance -= absAmount
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Balance

        if (confirmedBalance != other.confirmedBalance) return false
        if (pendingBalanceChanges != other.pendingBalanceChanges) return false

        return true
    }

    override fun hashCode(): Int {
        var result = confirmedBalance.hashCode()
        result = 31 * result + pendingBalanceChanges.hashCode()
        return result
    }

    override fun toString(): String {
        return "Balance(confirmedBalance=$confirmedBalance, pendingBalanceChanges=$pendingBalanceChanges)"
    }
}

fun Coin.formatted(): String = toString()

private operator fun Coin.plus(other: Coin): Coin = add(other)
private operator fun Coin.minus(other: Coin): Coin = subtract(other)

private fun Coin.abs(): Coin = if (this > Coin.ZERO) {
    this
} else {
    negate()
}

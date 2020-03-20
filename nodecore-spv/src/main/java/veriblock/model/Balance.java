// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.sdk.models.Coin;

public class Balance {
    private Coin confirmedBalance;

    public Coin getConfirmedBalance() {
        return confirmedBalance;
    }

    public void setConfirmedBalance(Coin confirmedBalance) {
        this.confirmedBalance = confirmedBalance;
    }

    private Coin pendingBalanceChanges;

    public Coin getPendingBalanceChanges() {
        return pendingBalanceChanges;
    }

    public Coin getPendingBalance() {
        return confirmedBalance.add(pendingBalanceChanges);
    }

    public Balance(Coin confirmedBalance) {
        this(confirmedBalance, Coin.ZERO);
    }

    public Balance(Coin confirmedBalance, Coin pendingBalanceChanges) {
        this.confirmedBalance = confirmedBalance;
        this.pendingBalanceChanges = pendingBalanceChanges;
    }

    public void addPendingSpend(Coin amount) {
        if (Coin.ZERO.compareTo(amount) > 0) {
            amount = amount.negate();
        }

        this.pendingBalanceChanges = pendingBalanceChanges.subtract(amount);
    }

    public void addPendingReceipt(Coin amount) {
        if (Coin.ZERO.compareTo(amount) > 0) {
            amount = amount.negate();
        }

        this.pendingBalanceChanges = pendingBalanceChanges.add(amount);
    }

    public void confirmPendingSpend(Coin amount) {
        if (Coin.ZERO.compareTo(amount) > 0) {
            amount = amount.negate();
        }

        this.pendingBalanceChanges = pendingBalanceChanges.add(amount);
        this.confirmedBalance = confirmedBalance.subtract(amount);
    }

    public void confirmPendingReceipt(Coin amount) {
        if (Coin.ZERO.compareTo(amount) > 0) {
            amount = amount.negate();
        }

        this.pendingBalanceChanges = pendingBalanceChanges.subtract(amount);
        this.confirmedBalance = confirmedBalance.add(amount);
    }

    public void addConfirmedSpend(Coin amount) {
        if (Coin.ZERO.compareTo(amount) > 0) {
            amount = amount.negate();
        }

        this.confirmedBalance = confirmedBalance.subtract(amount);
    }

    public void addConfirmedReceipt(Coin amount) {
        if (Coin.ZERO.compareTo(amount) > 0) {
            amount = amount.negate();
        }

        this.confirmedBalance = confirmedBalance.add(amount);
    }

    public void makeConfirmedSpendPending(Coin amount) {
        if (Coin.ZERO.compareTo(amount) > 0) {
            amount = amount.negate();
        }

        this.pendingBalanceChanges = pendingBalanceChanges.subtract(amount);
        this.confirmedBalance = confirmedBalance.add(amount);
    }

    public void makeConfirmedReceiptPending(Coin amount) {
        if (Coin.ZERO.compareTo(amount) > 0) {
            amount = amount.negate();
        }

        this.pendingBalanceChanges = pendingBalanceChanges.add(amount);
        this.confirmedBalance = confirmedBalance.subtract(amount);
    }

    public Balance clone() {
        return new Balance(this.confirmedBalance, this.pendingBalanceChanges);
    }

    public static Balance create() {
        return new Balance(Coin.ZERO, Coin.ZERO);
    }
}

// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.events;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;

public class CoinsReceivedEvent {
    private final Transaction tx;

    public Transaction getTx() {
        return tx;
    }

    private final Coin previousBalance;

    public Coin getPreviousBalance() {
        return previousBalance;
    }

    private final Coin newBalance;

    public Coin getNewBalance() {
        return newBalance;
    }

    public CoinsReceivedEvent(Transaction tx, Coin previousBalance, Coin newBalance) {
        this.tx = tx;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
    }
}

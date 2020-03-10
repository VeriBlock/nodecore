// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.events

import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction

class CoinsReceivedEvent(
    val tx: Transaction,
    val previousBalance: Coin,
    val newBalance: Coin
)

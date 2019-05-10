// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.events;

public class BitcoinServiceReadyEvent extends SuccessMessageEvent {
    public BitcoinServiceReadyEvent() {
        super("Bitcoin service is ready");
    }
}

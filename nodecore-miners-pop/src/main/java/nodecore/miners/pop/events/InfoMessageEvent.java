// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.events;

import nodecore.miners.pop.contracts.MessageEvent;

public class InfoMessageEvent implements MessageEvent {

    private final String message;

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Level getLevel() {
        return Level.INFO;
    }

    public InfoMessageEvent(String message) {
        this.message = message;
    }
}

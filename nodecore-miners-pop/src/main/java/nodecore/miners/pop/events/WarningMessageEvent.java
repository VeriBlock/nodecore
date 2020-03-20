// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.events;

import nodecore.miners.pop.contracts.MessageEvent;

public class WarningMessageEvent implements MessageEvent {
    private final String message;

    @Override
    public Level getLevel() {
        return Level.WARN;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public WarningMessageEvent(String message) {
        this.message = message;
    }
}

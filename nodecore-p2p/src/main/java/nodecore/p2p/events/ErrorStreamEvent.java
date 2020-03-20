// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.p2p.Peer;

public class ErrorStreamEvent extends StreamEvent<Throwable> {
    private final Throwable t;

    @Override
    public Throwable getContent() {
        return t;
    }

    public ErrorStreamEvent(Peer producer, Throwable t) {
        super(producer, "", false);
        this.t = t;
    }
}

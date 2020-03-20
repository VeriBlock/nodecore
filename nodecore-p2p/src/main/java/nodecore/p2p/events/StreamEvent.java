// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.p2p.Peer;

public abstract class StreamEvent<T> {
    private final Peer producer;
    private final String messageId;
    private final boolean acknowledgeRequested;

    public boolean acknowledgeRequested() {
        return acknowledgeRequested;
    }

    public Peer getProducer() {
        return producer;
    }

    public String getMessageId() {
        return messageId;
    }

    abstract T getContent();

    public StreamEvent(Peer producer, String messageId, boolean acknowledgeRequested) {
        this.producer = producer;
        this.messageId = messageId;
        this.acknowledgeRequested = acknowledgeRequested;
    }
}

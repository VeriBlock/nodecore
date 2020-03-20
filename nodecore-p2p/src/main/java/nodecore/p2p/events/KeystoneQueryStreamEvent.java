// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class KeystoneQueryStreamEvent extends StreamEvent<VeriBlockMessages.KeystoneQuery> {
    private final VeriBlockMessages.KeystoneQuery keystoneQuery;

    @Override
    public VeriBlockMessages.KeystoneQuery getContent() {
        return keystoneQuery;
    }

    public KeystoneQueryStreamEvent(Peer producer,
                                    String messageId,
                                    boolean acknowledgeRequested,
                                    VeriBlockMessages.KeystoneQuery keystoneQuery) {
        super(producer, messageId, acknowledgeRequested);
        this.keystoneQuery = keystoneQuery;
    }
}

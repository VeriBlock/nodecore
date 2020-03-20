// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import nodecore.api.grpc.VeriBlockMessages;

public class TransactionRequest implements PeerRequest {
    private final String txId;
    public String getTxId() {
        return txId;
    }

    private final VeriBlockMessages.TransactionAnnounce transaction;
    public VeriBlockMessages.TransactionAnnounce getTransaction() {
        return transaction;
    }

    private final Peer peer;
    @Override
    public Peer getPeer() {
        return peer;
    }

    private int requestedAt;
    @Override
    public int getRequestedAt() {
        return requestedAt;
    }
    @Override
    public void setRequestedAt(int requestedAt) {
        this.requestedAt = requestedAt;
    }

    public TransactionRequest(String txId, VeriBlockMessages.TransactionAnnounce transaction, Peer peer) {
        this.txId = txId;
        this.transaction = transaction;
        this.peer = peer;
    }
}

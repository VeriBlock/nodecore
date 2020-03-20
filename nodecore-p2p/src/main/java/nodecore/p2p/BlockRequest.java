// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import nodecore.api.grpc.VeriBlockMessages;

public class BlockRequest implements PeerRequest {
    private final String hash;
    public String getHash() {
        return hash;
    }

    private final VeriBlockMessages.BlockHeader header;
    public VeriBlockMessages.BlockHeader getHeader() {
        return header;
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

    public BlockRequest(String hash, VeriBlockMessages.BlockHeader header, Peer peer) {
        this.hash = hash;
        this.header = header;
        this.peer = peer;
    }
}

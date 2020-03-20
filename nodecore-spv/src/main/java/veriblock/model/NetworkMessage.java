// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import nodecore.api.grpc.VeriBlockMessages;
import veriblock.net.Peer;

public class NetworkMessage {
    private Peer sender;
    public Peer getSender() {
        return sender;
    }

    private VeriBlockMessages.Event message;
    public VeriBlockMessages.Event getMessage() {
        return message;
    }

    public NetworkMessage(Peer sender, VeriBlockMessages.Event message) {
        this.sender = sender;
        this.message = message;
    }
}

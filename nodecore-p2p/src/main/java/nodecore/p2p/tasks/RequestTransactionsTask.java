// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.tasks;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.MessageIdGenerator;
import nodecore.p2p.Peer;
import nodecore.p2p.TransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

public class RequestTransactionsTask implements PublishTask {
    private static final Logger logger = LoggerFactory.getLogger(RequestTransactionsTask.class);

    private final List<TransactionRequest> requests;

    public RequestTransactionsTask(List<TransactionRequest> requests) {
        this.requests = requests;
    }

    @Override
    public void execute() {
        if (requests == null || requests.size() == 0) return;

        HashMap<Peer, VeriBlockMessages.TransactionRequest.Builder> peerEvents = new HashMap<>();
        for (TransactionRequest req : requests) {
            if (!peerEvents.containsKey(req.getPeer())) {
                peerEvents.put(req.getPeer(), VeriBlockMessages.TransactionRequest.newBuilder());
            }

            peerEvents.get(req.getPeer()).addTransactions(req.getTransaction());
        }

        for (Peer peer : peerEvents.keySet()) {
            try {
                peer.send(VeriBlockMessages.Event.newBuilder()
                        .setId(MessageIdGenerator.next())
                        .setAcknowledge(false)
                        .setTxRequest(peerEvents.get(peer).build())
                        .build());
            } catch (Exception e) {
                logger.error("Unable to send transaction request", e);
            }
        }
    }
}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RequestPeerTableTask implements PublishTask {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatTask.class);

    private final List<Peer> targets;

    public RequestPeerTableTask(List<Peer> targets) {
        this.targets = targets;
    }

    @Override
    public void execute() {
        VeriBlockMessages.Event event = VeriBlockMessages.Event.newBuilder()
                .setId(MessageIdGenerator.next())
                .setAcknowledge(false)
                .setNetworkInfoRequest(VeriBlockMessages.NetworkInfoRequest.newBuilder().build())
                .build();

        targets.parallelStream().forEach(target -> {
            try {
                target.send(event);
            } catch (Exception e) {
                logger.error("Unable to request network info", e);
            }
        });
    }
}

// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.tasks;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;
import nodecore.p2p.MessageIdGenerator;
import nodecore.p2p.Peer;
import nodecore.p2p.model.BlockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HeartbeatTask implements PublishTask {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatTask.class);

    private final List<Peer> targets;
    private final BlockInfo blockInfo;

    public HeartbeatTask(List<Peer> targets, BlockInfo blockInfo) {
        this.targets = targets;
        this.blockInfo = blockInfo;
    }

    @Override
    public void execute() {
        VeriBlockMessages.Event event = VeriBlockMessages.Event.newBuilder()
                .setId(MessageIdGenerator.next())
                .setAcknowledge(false)
                .setHeartbeat(VeriBlockMessages.Heartbeat.newBuilder()
                        .setBlock(VeriBlockMessages.BlockInfo.newBuilder()
                                .setNumber(blockInfo.getNumber())
                                .setHash(ByteStringUtility.hexToByteString(blockInfo.getHash()))
                                .build()))
                .build();

        targets.parallelStream().forEach(target -> {
            try {
                target.send(event);
            } catch (Exception e) {
                logger.error("Unable to send heartbeat", e);
            }
        });
    }
}

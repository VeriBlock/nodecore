// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.tasks;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.MessageIdGenerator;
import nodecore.p2p.NodeMetadata;
import nodecore.p2p.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AnnounceTask implements PublishTask {
    private static final Logger logger = LoggerFactory.getLogger(AnnounceTask.class);

    private final List<Peer> targets;
    private final NodeMetadata self;
    private final boolean requestReply;
    
    public AnnounceTask(List<Peer> targets, NodeMetadata self, boolean requestReply) {
        this.targets = targets;
        this.self = self; 
        this.requestReply = requestReply;
    }
    
    @Override
    public void execute() {
        VeriBlockMessages.Event event = VeriBlockMessages.Event.newBuilder()
                .setId(MessageIdGenerator.next())
                .setAcknowledge(false)
                .setAnnounce(VeriBlockMessages.Announce.newBuilder()
                        .setReply(requestReply)
                        .setNodeInfo(VeriBlockMessages.NodeInfo.newBuilder()
                                .setApplication(self.getApplication())
                                .setProtocolVersion(self.getProtocolVersion())
                                .setPlatform(self.getPlatform())
                                .setStartTimestamp(self.getStartTimestamp())
                                .setShare(self.shareAddress())
                                .setCapabilities(self.getCapabilities())
                                .setId(self.getId())
                                .setPort(self.getPort())
                                .build())
                        .build())
                .build();

        targets.parallelStream().forEach(target -> {
            try {
                target.send(event);
            } catch (Exception e) {
                logger.error("Unable to send announce", e);
            }
        });
    }
}

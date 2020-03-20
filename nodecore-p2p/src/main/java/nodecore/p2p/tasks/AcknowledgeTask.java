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

public class AcknowledgeTask implements PublishTask {
    private static final Logger logger = LoggerFactory.getLogger(AcknowledgeTask.class);

    private final Peer target;
    public Peer getTarget() {
        return target;
    }

    private final String messageId;
    public String getMessageId() {
        return messageId;
    }

    public AcknowledgeTask(Peer target, String messageId) {
        this.target = target;
        this.messageId = messageId;
    }

    @Override
    public void execute() {
        VeriBlockMessages.Event event = VeriBlockMessages.Event.newBuilder()
                .setId(MessageIdGenerator.next())
                .setAcknowledge(false)
                .setAcknowledgement(VeriBlockMessages.Acknowledgement.newBuilder()
                        .setMessageId(messageId)
                        .build())
                .build();

        try {
            target.send(event);
        } catch (Exception e) {
            logger.error("Unable to send acknowledgment", e);
        }
    }
}

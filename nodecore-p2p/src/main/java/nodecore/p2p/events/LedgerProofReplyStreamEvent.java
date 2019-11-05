package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class LedgerProofReplyStreamEvent extends StreamEvent<VeriBlockMessages.LedgerProofReply> {
    private final VeriBlockMessages.LedgerProofReply ledgerProofReply;

    @Override
    public VeriBlockMessages.LedgerProofReply getContent() {
        return ledgerProofReply;
    }

    public LedgerProofReplyStreamEvent(Peer producer,
                                         String messageId,
                                         boolean acknowledgeRequested,
                                         VeriBlockMessages.LedgerProofReply ledgerProofReply) {
        super(producer, messageId, acknowledgeRequested);
        this.ledgerProofReply = ledgerProofReply;
    }
}
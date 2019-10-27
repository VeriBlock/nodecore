package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class LedgerProofRequestStreamEvent extends StreamEvent<VeriBlockMessages.LedgerProofRequest> {
    private final VeriBlockMessages.LedgerProofRequest ledgerProofRequest;

    @Override
    public VeriBlockMessages.LedgerProofRequest getContent() {
        return ledgerProofRequest;
    }

    public LedgerProofRequestStreamEvent(Peer producer,
                                           String messageId,
                                           boolean acknowledgeRequested,
                                           VeriBlockMessages.LedgerProofRequest ledgerProofRequest) {
        super(producer, messageId, acknowledgeRequested);
        this.ledgerProofRequest = ledgerProofRequest;
    }
}

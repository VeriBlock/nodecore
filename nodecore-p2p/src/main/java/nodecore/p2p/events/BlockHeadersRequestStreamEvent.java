package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class BlockHeadersRequestStreamEvent extends StreamEvent<VeriBlockMessages.BlockHeadersRequest> {
    private final VeriBlockMessages.BlockHeadersRequest blockHeadersRequest;

    @Override
    public VeriBlockMessages.BlockHeadersRequest getContent() {
        return blockHeadersRequest;
    }

    public BlockHeadersRequestStreamEvent(Peer producer,
                                           String messageId,
                                           boolean acknowledgeRequested,
                                           VeriBlockMessages.BlockHeadersRequest blockHeadersRequest) {
        super(producer, messageId, acknowledgeRequested);
        this.blockHeadersRequest = blockHeadersRequest;
    }
}

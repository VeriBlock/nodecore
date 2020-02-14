package veriblock.net;

import org.veriblock.sdk.models.Sha256Hash;

import java.util.List;

/**
 * Peer to peer service. Process requests/responses.
 */
public interface P2PService {

    void onTransactionRequest(List<Sha256Hash> txIds, Peer sender);

}

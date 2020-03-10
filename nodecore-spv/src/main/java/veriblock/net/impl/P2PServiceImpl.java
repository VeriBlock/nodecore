package veriblock.net.impl;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.conf.NetworkParameters;
import veriblock.model.Transaction;
import veriblock.net.P2PService;
import veriblock.net.Peer;
import veriblock.service.PendingTransactionContainer;
import veriblock.util.MessageIdGenerator;

import java.util.List;

public class P2PServiceImpl implements P2PService {
    private static final Logger logger = LoggerFactory.getLogger(P2PServiceImpl.class);

    private final PendingTransactionContainer pendingTransactionContainer;
    private final NetworkParameters networkParameters;

    public P2PServiceImpl(PendingTransactionContainer pendingTransactionContainer, NetworkParameters networkParameters) {
        this.pendingTransactionContainer = pendingTransactionContainer;
        this.networkParameters = networkParameters;
    }

    @Override
    public void onTransactionRequest(List<Sha256Hash> txIds, Peer sender) {
        for (Sha256Hash txId : txIds) {
            Transaction transaction = pendingTransactionContainer.getTransaction(txId);

            if (transaction != null) {
                logger.info("Found a transaction for the given transaction id: " + txId);
                VeriBlockMessages.Event.Builder builder = VeriBlockMessages.Event.newBuilder()
                        .setId(MessageIdGenerator.next())
                        .setAcknowledge(false);

                switch (transaction.getTransactionTypeIdentifier()) {
                    case STANDARD:
                        builder.setTransaction(
                            VeriBlockMessages.TransactionUnion.newBuilder().setSigned(transaction.getSignedMessageBuilder(networkParameters)));
                        break;
                    case MULTISIG:
                        //TODO SPV-47
                        throw new UnsupportedOperationException();

                    case PROOF_OF_PROOF:
                        builder.setTransaction(
                            VeriBlockMessages.TransactionUnion.newBuilder().setSigned(transaction.getSignedMessageBuilder(networkParameters)));
                        break;
                }

                try {
                    sender.sendMessage(builder.build());
                } catch (Exception e) {
                    logger.error("Unable to respond to transaction request", e);
                    return;
                }
            } else {
                logger.info("Couldn't find a transaction for the given id " + txId);
                VeriBlockMessages.Event.Builder builder = VeriBlockMessages.Event.newBuilder()
                        .setId(MessageIdGenerator.next())
                        .setAcknowledge(false)
                        .setNotFound(VeriBlockMessages.NotFound.newBuilder()
                                .setId(ByteString.copyFrom(txId.getBytes()))
                                .setType(VeriBlockMessages.NotFound.Type.TX));
                try {
                    sender.sendMessage(builder.build());
                } catch (Exception e) {
                    logger.error("Unable to respond to transaction request", e);
                    return;
                }
            }
        }
    }
}

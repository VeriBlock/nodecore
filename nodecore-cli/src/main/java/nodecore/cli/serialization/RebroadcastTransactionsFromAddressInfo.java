package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RebroadcastTransactionsFromAddressInfo {
    public RebroadcastTransactionsFromAddressInfo(final VeriBlockMessages.RebroadcastTransactionReply reply) {
        rebroadcastTransactionsCount = reply.getRebroadcastTransactionsCount();

        List<VeriBlockMessages.TransactionUnion> rebroadcastTransactionMessages = reply.getRebroadcastTransactionsList();

        List<VeriBlockMessages.Transaction> rawRebroadcastTransactions = new ArrayList<>();
        for (VeriBlockMessages.TransactionUnion rebroadcastTransaction : rebroadcastTransactionMessages) {
            if (rebroadcastTransaction.hasSigned()) {
                rawRebroadcastTransactions.add(rebroadcastTransaction.getSigned().getTransaction());
            } else {
                rawRebroadcastTransactions.add(rebroadcastTransaction.getSignedMultisig().getTransaction());
            }
        }

        this.rebroadcastTransactions = rawRebroadcastTransactions
                .stream()
                .map(TransactionInfo::new).collect(Collectors.toList());
    }

    public int rebroadcastTransactionsCount;

    public List<TransactionInfo> rebroadcastTransactions;
}

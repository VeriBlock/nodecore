package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AbandonTransactionsFromAddressInfo {
    public AbandonTransactionsFromAddressInfo(final VeriBlockMessages.AbandonTransactionReply reply) {
        abandonedTransactionsCount = reply.getAbandonedTransactionsCount();

        List<VeriBlockMessages.TransactionUnion> abandonedTransactionMessages = reply.getAbandonedTransactionsList();

        List<VeriBlockMessages.Transaction> rawAbandonedTransactions = new ArrayList<>();
        for (VeriBlockMessages.TransactionUnion abandonedTransaction : abandonedTransactionMessages) {
            if (abandonedTransaction.hasSigned()) {
                rawAbandonedTransactions.add(abandonedTransaction.getSigned().getTransaction());
            } else {
                rawAbandonedTransactions.add(abandonedTransaction.getSignedMultisig().getTransaction());
            }
        }

        this.abandonedTransactions = rawAbandonedTransactions
                .stream()
                .map(TransactionInfo::new).collect(Collectors.toList());
    }

    public int abandonedTransactionsCount;

    public List<TransactionInfo> abandonedTransactions;
}

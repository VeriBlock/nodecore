package nodecore.cli.serialization;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AbandonTransactionFromTxIDInfo {
    public AbandonTransactionFromTxIDInfo(final VeriBlockMessages.AbandonTransactionReply reply) {
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

        notFoundTransactionsCount = reply.getTxidsNotAbandonedCount();

        txidsNotAbandoned = new ArrayList<>();

        List<ByteString> txidsNotAbandonedByteStrings = reply.getTxidsNotAbandonedList();
        for (int i = 0; i < txidsNotAbandonedByteStrings.size(); i++) {
            txidsNotAbandoned.add(Utility.bytesToHex(txidsNotAbandonedByteStrings.get(i).toByteArray()));
        }

        this.abandonedTransactions = rawAbandonedTransactions
                .stream()
                .map(TransactionInfo::new).collect(Collectors.toList());
    }

    public int abandonedTransactionsCount;

    public int notFoundTransactionsCount;

    public List<TransactionInfo> abandonedTransactions;

    public List<String> txidsNotAbandoned;
}

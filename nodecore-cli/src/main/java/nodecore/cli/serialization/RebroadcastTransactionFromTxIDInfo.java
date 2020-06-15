package nodecore.cli.serialization;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RebroadcastTransactionFromTxIDInfo {
    public RebroadcastTransactionFromTxIDInfo(final VeriBlockMessages.RebroadcastTransactionReply reply) {
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

        notFoundTransactionsCount = reply.getTxidsNotRebroadcastCount();

        txidsNotRebroadcast = new ArrayList<>();

        List<ByteString> txidsNotRebroadcastByteStrings = reply.getTxidsNotRebroadcastList();
        for (int i = 0; i < txidsNotRebroadcastByteStrings.size(); i++) {
            txidsNotRebroadcast.add(Utility.bytesToHex(txidsNotRebroadcastByteStrings.get(i).toByteArray()));
        }

        this.rebroadcastTransactions = rawRebroadcastTransactions
                .stream()
                .map(TransactionInfo::new).collect(Collectors.toList());
    }

    public int rebroadcastTransactionsCount;

    public int notFoundTransactionsCount;

    public List<TransactionInfo> rebroadcastTransactions;

    public List<String> txidsNotRebroadcast;
}

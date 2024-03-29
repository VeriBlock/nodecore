package nodecore.cli.serialization

import nodecore.api.grpc.RpcAbandonTransactionReply
import org.veriblock.core.utilities.Utility

class AbandonTransactionFromTxIDInfo(
    reply: RpcAbandonTransactionReply
) {
    val abandonedTransactionsCount = reply.abandonedTransactionsCount

    val notFoundTransactionsCount = reply.txidsNotAbandonedCount

    val abandonedTransactions = reply.abandonedTransactionsList.map { transactionUnion ->
        if (transactionUnion.hasSigned()) {
            TransactionInfo(transactionUnion.signed.transaction)
        } else {
            TransactionInfo(transactionUnion.signedMultisig.transaction)
        }
    }

    val txidsNotAbandoned = reply.txidsNotAbandonedList.map { transactionId ->
        Utility.bytesToHex(transactionId.toByteArray())
    }
}

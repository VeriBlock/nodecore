package nodecore.cli.serialization

import nodecore.api.grpc.RpcRebroadcastTransactionReply
import org.veriblock.core.utilities.extensions.toHex

class RebroadcastTransactionFromTxIDInfo(
    reply: RpcRebroadcastTransactionReply
) {
    val rebroadcastTransactionsCount = reply.rebroadcastTransactionsCount

    val notFoundTransactionsCount = reply.txidsNotRebroadcastCount

    val rebroadcastTransactions = reply.rebroadcastTransactionsList.map { transactionUnion ->
        if (transactionUnion.hasSigned()) {
            TransactionInfo(transactionUnion.signed.transaction)
        } else {
            TransactionInfo(transactionUnion.signedMultisig.transaction)
        }
    }

    val txidsNotRebroadcast = reply.txidsNotRebroadcastList.map {
        it.toByteArray().toHex()
    }
}

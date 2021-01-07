package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.RebroadcastTransactionReply

class RebroadcastTransactionsFromAddressInfo(
    reply: RebroadcastTransactionReply
) {
    val rebroadcastTransactionsCount = reply.rebroadcastTransactionsCount

    val rebroadcastTransactions = reply.rebroadcastTransactionsList.map { transactionUnion ->
        if (transactionUnion.hasSigned()) {
            TransactionInfo(transactionUnion.signed.transaction)
        } else {
            TransactionInfo(transactionUnion.signedMultisig.transaction)
        }
    }
}

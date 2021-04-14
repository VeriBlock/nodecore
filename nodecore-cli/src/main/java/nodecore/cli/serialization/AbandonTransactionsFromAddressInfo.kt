package nodecore.cli.serialization

import nodecore.api.grpc.RpcAbandonTransactionReply

class AbandonTransactionsFromAddressInfo(
    reply: RpcAbandonTransactionReply
) {
    val abandonedTransactionsCount = reply.abandonedTransactionsCount

    val abandonedTransactions = reply.abandonedTransactionsList.map { transactionUnion ->
        if (transactionUnion.hasSigned()) {
            TransactionInfo(transactionUnion.signed.transaction)
        } else {
            TransactionInfo(transactionUnion.signedMultisig.transaction)
        }
    }
}

package nodecore.cli.serialization

import nodecore.api.grpc.RpcRebroadcastTransactionReply

class RebroadcastTransactionsFromAddressInfo(
    reply: RpcRebroadcastTransactionReply
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

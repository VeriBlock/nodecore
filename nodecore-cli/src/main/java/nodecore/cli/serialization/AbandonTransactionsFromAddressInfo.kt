package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages.AbandonTransactionReply

class AbandonTransactionsFromAddressInfo(
    reply: AbandonTransactionReply
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

package veriblock.net

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.NetworkParameters
import veriblock.model.TransactionTypeIdentifier
import veriblock.service.PendingTransactionContainer
import veriblock.util.MessageIdGenerator.next

private val logger = createLogger {}

class P2PService(
    private val pendingTransactionContainer: PendingTransactionContainer,
    private val networkParameters: NetworkParameters
) {
    fun onTransactionRequest(txIds: List<Sha256Hash>, sender: Peer) {
        for (txId in txIds) {
            val transaction = pendingTransactionContainer.getTransaction(txId)
            if (transaction != null) {
                logger.debug("Found a transaction for the given transaction id: $txId")
                val builder = VeriBlockMessages.Event.newBuilder()
                    .setId(next())
                    .setAcknowledge(false)
                when (transaction.transactionTypeIdentifier) {
                    TransactionTypeIdentifier.STANDARD -> builder.setTransaction(
                        VeriBlockMessages.TransactionUnion.newBuilder().setSigned(transaction.getSignedMessageBuilder(networkParameters))
                    )
                    TransactionTypeIdentifier.MULTISIG -> throw UnsupportedOperationException()
                    TransactionTypeIdentifier.PROOF_OF_PROOF -> builder.setTransaction(
                        VeriBlockMessages.TransactionUnion.newBuilder().setSigned(transaction.getSignedMessageBuilder(networkParameters))
                    )
                }
                try {
                    sender.sendMessage(builder.build())
                } catch (e: Exception) {
                    logger.error("Unable to respond to transaction request", e)
                    return
                }
            } else {
                logger.debug("Couldn't find a transaction for the given id $txId")
                val builder = VeriBlockMessages.Event.newBuilder()
                    .setId(next())
                    .setAcknowledge(false)
                    .setNotFound(
                        VeriBlockMessages.NotFound.newBuilder()
                            .setId(ByteString.copyFrom(txId.bytes))
                            .setType(VeriBlockMessages.NotFound.Type.TX)
                    )
                try {
                    sender.sendMessage(builder.build())
                } catch (e: Exception) {
                    logger.error("Unable to respond to transaction request", e)
                    return
                }
            }
        }
    }
}

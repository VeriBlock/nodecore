package veriblock.service

import nodecore.api.grpc.VeriBlockMessages.SignedMultisigTransaction
import nodecore.api.grpc.VeriBlockMessages.SignedTransaction
import nodecore.api.grpc.VeriBlockMessages.TransactionUnion
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import org.slf4j.LoggerFactory
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Sha256Hash
import veriblock.conf.NetworkParameters
import veriblock.model.StandardTransaction
import veriblock.model.Transaction
import veriblock.wallet.WalletProtobufSerializer

private val logger = createLogger {}

class TransactionFactory(private val networkParameters: NetworkParameters) {
    fun create(message: TransactionUnion): Transaction? {
        return when (message.transactionCase) {
            TransactionUnion.TransactionCase.UNSIGNED -> throw IllegalArgumentException(
                "Cannot use this constructor with an Unsigned transaction type"
            )
            TransactionUnion.TransactionCase.SIGNED -> create(message.signed)
            TransactionUnion.TransactionCase.SIGNED_MULTISIG -> create(message.signedMultisig)
            TransactionUnion.TransactionCase.TRANSACTION_NOT_SET -> throw IllegalArgumentException(
                "Cannot use this constructor with an unset transaction type"
            )
            null -> null
        }
    }

    fun create(message: SignedTransaction): Transaction {
        val serializer = WalletProtobufSerializer()
        val transaction: Transaction = StandardTransaction(
            Sha256Hash.wrap(message.transaction.txId.toByteArray()),
            ByteStringAddressUtility.parseProperAddressTypeAutomatically(message.transaction.sourceAddress),
            message.transaction.sourceAmount,
            serializer.deserializeOutputs(message.transaction.outputsList),
            message.signatureIndex,
            message.transaction.data.toByteArray(),
            networkParameters
        )
        transaction.addSignature(message.signature.toByteArray(), message.publicKey.toByteArray())
        val valid = AddressUtility.isSignatureValid(
            transaction.txId!!.bytes, transaction.signature, transaction.publicKey, transaction.inputAddress!!.get()
        )
        if (!valid) {
            logger.error("Transaction signature is not valid.")
            throw RuntimeException()
        }
        return transaction
    }

    fun create(message: SignedMultisigTransaction?): Transaction {
        //TODO implement working with Multisig
//        setSignatureBundle(message.getSignatureBundle());
        throw UnsupportedOperationException()
    }
}

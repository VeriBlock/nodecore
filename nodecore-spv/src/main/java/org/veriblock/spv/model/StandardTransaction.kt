// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.SignedTransaction
import nodecore.api.grpc.utilities.extensions.asHexByteString
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.SerializerUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.services.serialize
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList

private val logger = createLogger {}

open class StandardTransaction : Transaction {
    var inputAmount: Coin? = null
    private val outputs: MutableList<Output> = ArrayList()
    private var signatureIndex: Long = 0
    private var transactionFee: Long = 0
    override var data: ByteArray? = null

    constructor(txId: Sha256Hash) : super(txId)

    constructor(
        inputAddress: String,
        inputAmount: Long,
        outputs: List<Output>,
        signatureIndex: Long,
        networkParameters: NetworkParameters
    ) : this(
        null, inputAddress, inputAmount, outputs, signatureIndex, ByteArray(0), networkParameters
    )

    constructor(
        txId: Sha256Hash?,
        inputAddress: String,
        inputAmount: Long,
        outputs: List<Output>,
        signatureIndex: Long,
        data: ByteArray?,
        networkParameters: NetworkParameters
    ) {
        var totalOutput = 0L
        for (o in outputs) {
            totalOutput += o.amount.atomicUnits
        }
        val fee = inputAmount - totalOutput

        // Only for Alt Chain Endorsement Transactions
        this.data = data
        this.signatureIndex = signatureIndex
        addAllOutput(outputs)
        this.inputAmount = inputAmount.asCoin()
        this.inputAddress = StandardAddress(inputAddress)
        transactionFee = fee
        if (txId == null) {
            this.txId = calculateTxId(networkParameters)
        } else {
            this.txId = txId
        }
    }

    override fun toByteArray(networkParameters: NetworkParameters): ByteArray {
        ByteArrayOutputStream().use { stream ->
            serializeToStream(stream, networkParameters)
            return stream.toByteArray()
        }
    }

    override fun getSignedMessageBuilder(networkParameters: NetworkParameters): SignedTransaction.Builder {
        val transaction = getTransactionMessageBuilder(networkParameters).build()
        val builder = SignedTransaction.newBuilder()
        builder.transaction = transaction
        builder.signatureIndex = signatureIndex
        builder.publicKey = ByteString.copyFrom(publicKey)
        builder.signature = ByteString.copyFrom(signature)
        return builder
    }

    private fun getTransactionMessageBuilder(networkParameters: NetworkParameters): VeriBlockMessages.Transaction.Builder {
        val builder = VeriBlockMessages.Transaction.newBuilder()
        builder.timestamp = Utility.getCurrentTimeSeconds()
        builder.transactionFee = transactionFee
        builder.txId = txId.toString().asHexByteString()
        if (transactionTypeIdentifier == TransactionTypeIdentifier.STANDARD) {
            builder.type = VeriBlockMessages.Transaction.Type.STANDARD
        } else if (transactionTypeIdentifier == TransactionTypeIdentifier.MULTISIG) {
            builder.type = VeriBlockMessages.Transaction.Type.MULTISIG
        }
        builder.sourceAmount = inputAmount!!.atomicUnits
        builder.sourceAddress = ByteString.copyFrom(inputAddress!!.toByteArray())
        builder.data = ByteString.copyFrom(data)
        builder.size = toByteArray(networkParameters).size
        for (output in getOutputs()) {
            val outputBuilder = builder.addOutputsBuilder()
            outputBuilder.address = ByteString.copyFrom(output.address.toByteArray())
            outputBuilder.amount = output.amount.atomicUnits
        }
        return builder
    }

    @Throws(IOException::class)
    private fun serializeToStream(stream: OutputStream, networkParameters: NetworkParameters) {
        val magicByte = networkParameters.transactionPrefix
        if (magicByte != null) {
            stream.write(magicByte.toInt())
        }

        // Write type
        stream.write(transactionTypeIdentifier.id.toInt())

        // Write source address
        inputAddress!!.serializeToStream(stream)

        // Write source amount
        stream.serialize(inputAmount!!)

        // Write destinations
        stream.write(getOutputs().size)
        for (output in getOutputs()) {
            output.serializeToStream(stream)
        }
        SerializerUtility.writeVariableLengthValueToStream(stream, signatureIndex)
        SerializerUtility.writeVariableLengthValueToStream(stream, data)
    }

    override fun getOutputs(): List<Output> {
        return outputs
    }

    fun addOutput(o: Output) {
        outputs.add(o)
    }

    fun addAllOutput(o: Collection<Output>) {
        outputs.addAll(o)
    }

    override fun getSignatureIndex(): Long =
        signatureIndex

    fun setSignatureIndex(signatureIndex: Long) {
        this.signatureIndex = signatureIndex
    }

    override fun getTransactionFee(): Long =
        transactionFee

    override val transactionTypeIdentifier: TransactionTypeIdentifier
        get() = TransactionTypeIdentifier.STANDARD

    private fun calculateTxId(
        networkParameters: NetworkParameters
    ): Sha256Hash {
        return Sha256Hash.wrap(calculateTxIDBytes(toByteArray(networkParameters)))
    }

    private fun calculateTxIDBytes(rawTx: ByteArray): ByteArray {
        return Crypto().SHA256ReturnBytes(rawTx)
    }
}

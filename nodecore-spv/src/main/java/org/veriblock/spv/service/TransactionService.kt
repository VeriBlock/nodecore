// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.service

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.types.Pair
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.wallet.AddressManager
import org.veriblock.sdk.models.asCoin
import org.veriblock.spv.model.AddressCoinsIndex
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.SigningResult
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.Transaction
import java.util.ArrayList

class TransactionService(
    private val addressManager: AddressManager,
    private val networkParameters: NetworkParameters
) {
    fun calculateFee(
        requestedSourceAddress: String,
        totalOutputAmount: Long,
        outputList: List<Output>,
        signatureIndex: Long
    ): Long {
        // This is for over-estimating the size of the transaction by one byte in the edge case where totalOutputAmount
        // is right below a power-of-two barrier
        val feeFudgeFactor = DEFAULT_TRANSACTION_FEE * 500L
        val predictedTransactionSize = predictStandardTransactionToAllStandardOutputSize(
            totalOutputAmount + feeFudgeFactor, outputList, signatureIndex + 1, 0
        )
        return predictedTransactionSize * DEFAULT_TRANSACTION_FEE
    }

    fun createTransactionsByOutputList(
        addressCoinsIndexList: List<AddressCoinsIndex>,
        outputList: List<Output>
    ): List<Transaction> {
        val transactions: MutableList<Transaction> = ArrayList()
        var sortedOutputs = outputList.sortedBy { it.amount.atomicUnits }.toMutableList()
        val sortedAddressCoinsIndexList = addressCoinsIndexList.filter { it.coins > 0 }.sortedBy { it.coins }
        val totalOutputAmount = sortedOutputs.map { it.amount.atomicUnits }.sum()
        for (sourceAddressesIndex in sortedAddressCoinsIndexList) {
            val fee = calculateFee(sourceAddressesIndex.address, totalOutputAmount, sortedOutputs, sourceAddressesIndex.index)
            val fulfillAndForPay = splitOutPutsAccordingBalance(
                sortedOutputs, (sourceAddressesIndex.coins - fee).asCoin()
            )
            sortedOutputs = fulfillAndForPay.first
            val outputsForPay = fulfillAndForPay.second
            val transactionInputAmount = outputsForPay.map {
                it.amount.atomicUnits
            }.sum() + fee
            val transaction = createStandardTransaction(
                sourceAddressesIndex.address, transactionInputAmount, outputsForPay, sourceAddressesIndex.index + 1
            )
            transactions.add(transaction)
            if (sortedOutputs.size == 0) {
                break
            }
        }
        return transactions
    }

    fun splitOutPutsAccordingBalance(
        outputs: MutableList<Output>,
        balance: Coin
    ): Pair<MutableList<Output>, List<Output>> {
        val outputsLeft: MutableList<Output> = ArrayList()
        val outputsForPay: MutableList<Output> = ArrayList()
        var balanceLeft = balance
        for (i in outputs.indices) {
            val output = outputs[i]
            balanceLeft = if (balanceLeft.atomicUnits > output.amount.atomicUnits) {
                outputsForPay.add(output)
                balance.subtract(output.amount)
            } else {
                val partForPay = Output(output.address, balanceLeft)
                outputsForPay.add(partForPay)
                val leftToPay = output.amount.subtract(balanceLeft)
                outputs.add(Output(output.address, leftToPay))
                outputs.remove(output)
                for (j in i until outputs.size) {
                    outputsLeft.add(outputs[j])
                }
                return Pair(
                    outputsLeft, outputsForPay
                )
            }
        }
        return Pair(
            outputsLeft, outputsForPay
        )
    }

    /**
     * Create a standard signed transaction.
     *
     * @param inputAddress   The address tokens are being spent from
     * @param inputAmount    The quantity of tokens being removed from the input address
     * @param outputs        All _outputs (with their corresponding share of the sent tokens)
     * @param signatureIndex The index of signature for inputAddress
     * @return A StandardTransaction object
     */
    fun createStandardTransaction(
        inputAddress: String,
        inputAmount: Long,
        outputs: List<Output>,
        signatureIndex: Long
    ): Transaction {
        require(AddressUtility.isValidStandardAddress(inputAddress)) {
            "createStandardTransaction cannot be called with an invalid inputAddress ($inputAddress)!"
        }
        require(Utility.isPositive(inputAmount)) {
            "createStandardTransaction cannot be called with a non-positive inputAmount ($inputAmount)!"
        }
        var outputTotal = 0L
        for (outputCount in outputs.indices) {
            val output = outputs[outputCount]
            val outputAmount = output.amount.atomicUnits
            require(Utility.isPositive(outputAmount)) {
                "createStandardTransaction cannot be called with an output (at index $outputCount) with a non-positive output amount!"
            }
            outputTotal += outputAmount
        }
        require(outputTotal <= inputAmount) {
            "createStandardTransaction cannot be called with an output total which is larger than the inputAmount" +
                " (outputTotal = $outputTotal, inputAmount = $inputAmount)!"
        }
        val transaction: Transaction = StandardTransaction(inputAddress, inputAmount, outputs, signatureIndex, networkParameters)
        val signingResult = signTransaction(transaction.txId, inputAddress)
        if (signingResult.succeeded()) {
            transaction.addSignature(signingResult.signature, signingResult.publicKey)
        }
        return transaction
    }

    // TODO(warchant): use Address instead of String for all addresses
    fun createUnsignedAltChainEndorsementTransaction(
        inputAddress: String, fee: Long, publicationData: ByteArray?, signatureIndex: Long
    ): Transaction {
        require(AddressUtility.isValidStandardAddress(inputAddress)) {
            "createAltChainEndorsementTransaction cannot be called with an invalid inputAddress ($inputAddress)!"
        }
        require(Utility.isPositive(fee)) {
            "createAltChainEndorsementTransaction cannot be called with a non-positiveinputAmount ($fee)!"
        }
        return StandardTransaction(null, inputAddress, fee, emptyList(), signatureIndex, publicationData, networkParameters)
    }

    fun predictStandardTransactionToAllStandardOutputSize(
        inputAmount: Long,
        outputs: List<Output>,
        sigIndex: Long,
        extraDataLength: Int
    ): Int {
        var totalSize = 0
        totalSize += 1 // Transaction Version
        totalSize += 1 // Type of Input Address
        totalSize += 1 // Standard Input Address Length Byte
        totalSize += 22 // Standard Input Address Length
        val inputAmountBytes = Utility.trimmedByteArrayFromLong(inputAmount)
        totalSize += 1 // Input Amount Length Byte
        totalSize += inputAmountBytes.size // Input Amount Length
        totalSize += 1 // Number of Outputs
        for (i in outputs.indices) {
            totalSize += 1 // ID of Output Address
            totalSize += 1 // Output Address Length Bytes
            totalSize += 22 // Output Address Length
            val outputAmount = Utility.trimmedByteArrayFromLong(outputs[i].amount.atomicUnits)
            totalSize += 1 // Output Amount Length Bytes
            totalSize += outputAmount.size // Output Amount Length
        }
        val sigIndexBytes = Utility.trimmedByteArrayFromLong(sigIndex)
        totalSize += 1 // Sig Index Length Bytes
        totalSize += sigIndexBytes.size // Sig Index Bytes
        val dataLengthBytes = Utility.trimmedByteArrayFromInteger(extraDataLength)
        totalSize += 1 // Data Length Bytes Length
        totalSize += dataLengthBytes.size // Data Length Bytes
        totalSize += extraDataLength // Extra data section
        return totalSize
    }

    fun signTransaction(txId: Sha256Hash, address: String): SigningResult {
        val signature = addressManager.signMessage(txId.bytes, address)
            ?: return SigningResult(false, null, null)
        val publicKey = addressManager.getPublicKeyForAddress(address)
            ?: return SigningResult(false, null, null)
        return SigningResult(true, signature, publicKey.encoded)
    }

    companion object {
        private const val DEFAULT_TRANSACTION_FEE = 1000L
        @JvmStatic
        fun predictAltChainEndorsementTransactionSize(dataLength: Int, sigIndex: Long): Int {
            var totalSize = 0

            // Using an estimated total fee of 1 VBK
            val inputAmount = 100000000L
            totalSize += 1 // Transaction Version
            totalSize += 1 // Type of Input Address
            totalSize += 1 // Standard Input Address Length Byte
            totalSize += 22 // Standard Input Address Length
            val inputAmountBytes = Utility.trimmedByteArrayFromLong(inputAmount)
            val inputAmountLength = inputAmountBytes.size.toLong()
            totalSize += 1 // Input Amount Length Byte
            totalSize += inputAmountLength.toInt() // Input Amount Length
            totalSize += 1 // Number of Outputs, will be 0
            val sigIndexBytes = Utility.trimmedByteArrayFromLong(sigIndex)
            totalSize += 1 // Sig Index Length Bytes
            totalSize += sigIndexBytes.size // Sig Index Bytes
            val dataSizeBytes = Utility.trimmedByteArrayFromInteger(dataLength)
            totalSize += 1 // Data Length Bytes Length
            totalSize += dataSizeBytes.size // Data Length Bytes (value will be 0)
            totalSize += dataLength
            return totalSize
        }

        @JvmStatic
        fun getRegularTransactionMessageBuilder(tx: StandardTransaction): VeriBlockMessages.Transaction.Builder {
            val builder = VeriBlockMessages.Transaction.newBuilder()
            builder.transactionFee = tx.getTransactionFee()
            builder.txId = ByteString.copyFrom(tx.txId.bytes)
            builder.type = VeriBlockMessages.Transaction.Type.STANDARD
            builder.sourceAmount = tx.inputAmount!!.atomicUnits
            builder.sourceAddress = ByteString.copyFrom(tx.inputAddress!!.toByteArray())
            builder.data = ByteString.copyFrom(tx.data)
            //        builder.setTimestamp(getTimeStamp());
            //        builder.setSize(tx.getSize());
            for (output in tx.getOutputs()) {
                val outputBuilder = builder.addOutputsBuilder()
                outputBuilder.address = ByteString.copyFrom(output.address.toByteArray())
                outputBuilder.amount = output.amount.atomicUnits
            }
            return builder
        }
    }

}

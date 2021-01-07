// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.extensions.toHex
import nodecore.api.grpc.utilities.extensions.toProperAddressType
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex
import java.util.*

class TransactionInfo(
    transaction: VeriBlockMessages.Transaction
) {
    val size: Int
    val txid: String
    val data: String

    var type: String? = when (transaction.type) {
        VeriBlockMessages.Transaction.Type.STANDARD -> "standard"
        VeriBlockMessages.Transaction.Type.PROOF_OF_PROOF -> "proof_of_proof"
        VeriBlockMessages.Transaction.Type.MULTISIG ->  "multisig"
        else -> null
    }

    val fee: String
    val timestamp: Int

    @SerializedName("source_amount")
    val sourceAmount: String

    @SerializedName("merkle_path")
    val merklePath: String

    @SerializedName("source_address")
    val sourceAddress: String

    @SerializedName("bitcoin_transaction")
    val bitcoinTransaction: String

    @SerializedName("bitcoin_block_header_of_proof")
    val bitcoinBlockHeader: String

    @SerializedName("endorsed_block_header")
    val endorsedBlockHeader: String

    var outputs: MutableList<OutputInfo> = ArrayList()

    init {
        if (transaction !== VeriBlockMessages.Transaction.getDefaultInstance()) {
            txid = transaction.txId.toHex()
            merklePath = transaction.merklePath
            size = transaction.size
            timestamp = transaction.timestamp
            sourceAmount = transaction.sourceAmount.formatAtomicLongWithDecimal()
            sourceAddress = transaction.sourceAddress.toProperAddressType()
            data = transaction.data.toHex()
            fee = transaction.transactionFee.formatAtomicLongWithDecimal()

            if (transaction.type == VeriBlockMessages.Transaction.Type.PROOF_OF_PROOF) {
                var bitcoinBlockHeaderBytes = transaction.bitcoinBlockHeaderOfProof?.toByteArray() ?: byteArrayOf()
                if (bitcoinBlockHeaderBytes.isNotEmpty() && bitcoinBlockHeaderBytes[0].toInt() == 10 && bitcoinBlockHeaderBytes[1].toInt() == 80) {
                    //TODO: remove first two bytes if they are 0A50 (10,80)
                    val newBytes = ByteArray(bitcoinBlockHeaderBytes.size - 2)
                    System.arraycopy(bitcoinBlockHeaderBytes, 2, newBytes, 0, newBytes.size)
                    bitcoinBlockHeaderBytes = newBytes
                }
                bitcoinBlockHeader = bitcoinBlockHeaderBytes.toHex()
                bitcoinTransaction = transaction.bitcoinTransaction.toHex()
                endorsedBlockHeader = transaction.endorsedBlockHeader.toHex()
            } else {
                bitcoinBlockHeader = "N/A"
                bitcoinTransaction = "N/A"
                endorsedBlockHeader = "N/A"
            }
            transaction.outputsList.forEach { output ->
                outputs.add(OutputInfo(output))
            }
        } else {
            size = 0
            txid = "N/A"
            data = "N/A"
            type = "N/A"
            fee = "N/A"
            timestamp = 0
            sourceAmount = "N/A"
            merklePath = "N/A"
            sourceAddress = "N/A"
            bitcoinTransaction = "N/A"
            bitcoinBlockHeader = "N/A"
            endorsedBlockHeader = "N/A"
        }
    }
}

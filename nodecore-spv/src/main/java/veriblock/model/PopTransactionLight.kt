// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import nodecore.api.grpc.VeriBlockMessages.SignedTransaction
import org.veriblock.core.utilities.SerializerUtility
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.MerklePath
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import veriblock.conf.NetworkParameters
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList

class PopTransactionLight(txId: Sha256Hash) : StandardTransaction(txId) {
    var endorsedBlock: VeriBlockBlock? = null
    var bitcoinTx: BitcoinTransaction? = null
    var bitcoinMerklePath: MerklePath? = null
    var blockOfProof: BitcoinBlock? = null
    private val blockOfProofContext: MutableList<BitcoinBlock> = ArrayList()

    fun getContextBitcoinBlocks(): List<BitcoinBlock> =
        blockOfProofContext

    fun addContextBitcoinBlocks(contextBitcoinBlock: BitcoinBlock) {
        blockOfProofContext.add(contextBitcoinBlock)
    }

    override fun toByteArray(networkParameters: NetworkParameters): ByteArray {
        return calculateHash()
    }

    override fun getSignedMessageBuilder(networkParameters: NetworkParameters): SignedTransaction.Builder {
        TODO() // SPV-48
    }

    private fun calculateHash(): ByteArray {
        ByteArrayOutputStream().use { stream ->
            serializeToStream(stream)
            return stream.toByteArray()
        }
    }

    override val transactionTypeIdentifier: TransactionTypeIdentifier
        get() = TransactionTypeIdentifier.PROOF_OF_PROOF

    @Throws(IOException::class)
    private fun serializeToStream(stream: OutputStream) {
        stream.write(transactionTypeIdentifier.id.toInt())
        inputAddress!!.serializeToStream(stream)
        SerializeDeserializeService.serialize(endorsedBlock, stream)
        SerializeDeserializeService.serialize(bitcoinTx, stream)
        SerializeDeserializeService.serialize(bitcoinMerklePath, stream)
        SerializeDeserializeService.serialize(blockOfProof, stream)
        SerializerUtility.writeVariableLengthValueToStream(stream, getContextBitcoinBlocks().size)
        for (block in getContextBitcoinBlocks()) {
            SerializeDeserializeService.serialize(block, stream)
        }
    }
}

// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import nodecore.api.grpc.RpcSignedTransaction
import org.veriblock.core.utilities.SerializerUtility
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.MerklePath
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.params.NetworkParameters
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList

class PopTransactionLight(
    txId: VbkTxId,
    val endorsedBlock: VeriBlockBlock,
    val bitcoinTx: BitcoinTransaction,
    val bitcoinMerklePath: MerklePath,
    val blockOfProof: BitcoinBlock
) : StandardTransaction(txId) {
    private val blockOfProofContext: MutableList<BitcoinBlock> = ArrayList()

    fun getContextBitcoinBlocks(): List<BitcoinBlock> =
        blockOfProofContext

    fun addContextBitcoinBlocks(contextBitcoinBlock: BitcoinBlock) {
        blockOfProofContext.add(contextBitcoinBlock)
    }

    override fun toByteArray(networkParameters: NetworkParameters): ByteArray {
        return calculateHash()
    }

    override fun getSignedMessageBuilder(networkParameters: NetworkParameters): RpcSignedTransaction.Builder {
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

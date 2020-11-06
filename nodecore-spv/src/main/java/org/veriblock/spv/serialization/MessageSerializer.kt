// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.serialization

import com.google.protobuf.InvalidProtocolBufferException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.SignedMultisigTransaction
import nodecore.api.grpc.VeriBlockMessages.SignedTransaction
import nodecore.api.grpc.VeriBlockMessages.TransactionUnion
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.extensions.asVbkPreviousBlockHash
import nodecore.api.grpc.utilities.extensions.asVbkPreviousKeystoneHash
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.MerklePath
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.crypto.asBtcHash
import org.veriblock.core.crypto.asTruncatedMerkleRoot
import org.veriblock.core.crypto.asVbkTxId
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.spv.model.BlockMetaPackage
import org.veriblock.spv.model.FullBlock
import org.veriblock.spv.model.MultisigTransaction
import org.veriblock.spv.model.OutputFactory.create
import org.veriblock.spv.model.PopTransactionLight
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.asLightAddress

private val logger = createLogger {}

object MessageSerializer {
    fun deserialize(blockHeaderMessage: VeriBlockMessages.BlockHeader, trustHash: Boolean = false): VeriBlockBlock {
        return if (trustHash) {
            SerializeDeserializeService.parseVeriBlockBlock(blockHeaderMessage.header.toByteArray(), blockHeaderMessage.hash.toByteArray().asVbkHash())
        } else {
            SerializeDeserializeService.parseVeriBlockBlock(blockHeaderMessage.header.toByteArray())
        }
    }

    @JvmStatic
    fun deserializeNormalTransaction(transactionUnionMessage: TransactionUnion): StandardTransaction {
        return when (transactionUnionMessage.transactionCase) {
            TransactionUnion.TransactionCase.SIGNED -> deserializeStandardTransaction(
                transactionUnionMessage.signed
            )
            TransactionUnion.TransactionCase.SIGNED_MULTISIG -> deserializeMultisigTransaction(
                transactionUnionMessage.signedMultisig
            )
            else ->
                // Should be impossible
                error("Unhandled transaction type: ${transactionUnionMessage.transactionCase}")
        }
    }

    fun deserializePopTransaction(transactionUnionMessage: TransactionUnion): PopTransactionLight {
        val signed = transactionUnionMessage.signed
        val txMessage = signed.transaction
        val tx = PopTransactionLight(
            txId = txMessage.txId.toByteArray().asVbkTxId(),
            endorsedBlock = SerializeDeserializeService.parseVeriBlockBlock(txMessage.endorsedBlockHeader.toByteArray()),
            bitcoinTx = BitcoinTransaction(txMessage.bitcoinTransaction.toByteArray()),
            bitcoinMerklePath = MerklePath(txMessage.merklePath),
            blockOfProof = SerializeDeserializeService.parseBitcoinBlock(txMessage.bitcoinBlockHeaderOfProof.header.toByteArray())
        )
        tx.inputAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress).asLightAddress()
        txMessage.contextBitcoinBlockHeadersList.map {
            SerializeDeserializeService.parseBitcoinBlock(it.header.toByteArray())
        }.forEach {
            tx.addContextBitcoinBlocks(it)
        }
        return tx
    }

    fun deserialize(blockMessage: VeriBlockMessages.Block): FullBlock {
        val block = FullBlock(
            blockMessage.number,
            blockMessage.version.toShort(),
            blockMessage.previousHash.asVbkPreviousBlockHash(),
            blockMessage.secondPreviousHash.asVbkPreviousKeystoneHash(),
            blockMessage.thirdPreviousHash.asVbkPreviousKeystoneHash(),
            ByteStringUtility.byteStringToHex(blockMessage.merkleRoot).asTruncatedMerkleRoot(),
            blockMessage.timestamp,
            blockMessage.encodedDifficulty,
            blockMessage.winningNonce
        )
        block.normalTransactions = blockMessage.regularTransactionsList.map {
            deserializeNormalTransaction(it)
        }
        block.popTransactions = blockMessage.popTransactionsList.map {
            deserializePopTransaction(it)
        }
        block.metaPackage = BlockMetaPackage(
            blockMessage.blockContentMetapackage.hash.toByteArray().asBtcHash()
        )
        return block
    }

    private fun deserializeStandardTransaction(signedTransaction: SignedTransaction): StandardTransaction {
        val txMessage = signedTransaction.transaction
        val tx = StandardTransaction(txMessage.txId.toByteArray().asVbkTxId())
        tx.inputAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress).asLightAddress()
        tx.inputAmount = txMessage.sourceAmount.asCoin()
        txMessage.outputsList.map {
            create(ByteStringAddressUtility.parseProperAddressTypeAutomatically(it.address), it.amount)
        }.forEach {
            tx.addOutput(it)
        }
        tx.setSignatureIndex(signedTransaction.signatureIndex)
        tx.data = txMessage.data.toByteArray()
        return tx
    }

    private fun deserializeMultisigTransaction(signedTransaction: SignedMultisigTransaction): StandardTransaction {
        val txMessage = signedTransaction.transaction
        val tx: StandardTransaction = MultisigTransaction(txMessage.txId.toByteArray().asVbkTxId())
        tx.inputAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress).asLightAddress()
        tx.inputAmount = txMessage.sourceAmount.asCoin()
        txMessage.outputsList.map {
            create(ByteStringAddressUtility.parseProperAddressTypeAutomatically(it.address), it.amount)
        }.forEach {
            tx.addOutput(it)
        }
        tx.setSignatureIndex(signedTransaction.signatureIndex)
        tx.data = txMessage.data.toByteArray()
        return tx
    }

    @JvmStatic
    fun deserialize(raw: ByteArray?): VeriBlockMessages.Event? {
        try {
            return VeriBlockMessages.Event.parseFrom(raw)
        } catch (e: InvalidProtocolBufferException) {
            logger.error("Unable to parse message", e)
        }
        return null
    }
}

// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.serialization

import com.google.protobuf.InvalidProtocolBufferException
import nodecore.api.grpc.RpcBlock
import nodecore.api.grpc.RpcBlockHeader
import nodecore.api.grpc.RpcEvent
import nodecore.api.grpc.RpcSignedMultisigTransaction
import nodecore.api.grpc.RpcSignedTransaction
import nodecore.api.grpc.RpcTransactionUnion
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
import org.veriblock.core.utilities.AddressUtility
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
    private const val VBK_HASH_LENGTH_FULL = 24
    private const val VBK_HASH_LENGTH = 12
    private const val VBK_KEYSTONE_HASH_LENGTH = 9
    private const val TRUNCATED_MERKLE_ROOT_LENGTH = 16

    fun deserialize(blockHeaderMessage: RpcBlockHeader, trustHash: Boolean = false): VeriBlockBlock {
        return if (trustHash) {
            SerializeDeserializeService.parseVeriBlockBlock(blockHeaderMessage.header.toByteArray(), blockHeaderMessage.hash.toByteArray().asVbkHash())
        } else {
            SerializeDeserializeService.parseVeriBlockBlock(blockHeaderMessage.header.toByteArray())
        }
    }

    @JvmStatic
    fun deserializeNormalTransaction(transactionUnionMessage: RpcTransactionUnion): StandardTransaction {
        return when (transactionUnionMessage.transactionCase) {
            RpcTransactionUnion.TransactionCase.SIGNED -> deserializeStandardTransaction(
                transactionUnionMessage.signed
            )
            RpcTransactionUnion.TransactionCase.SIGNED_MULTISIG -> deserializeMultisigTransaction(
                transactionUnionMessage.signedMultisig
            )
            else ->
                // Should be impossible
                error("Unhandled transaction type: ${transactionUnionMessage.transactionCase}")
        }
    }

    fun deserializePopTransaction(transactionUnionMessage: RpcTransactionUnion): PopTransactionLight {
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

    fun deserialize(blockMessage: RpcBlock): FullBlock {
        val merkleRoot = ByteStringUtility.byteStringToHex(blockMessage.merkleRoot)
        val block = FullBlock(
            blockMessage.number,
            blockMessage.version.toShort(),

            if (blockMessage.previousHash.size() == VBK_HASH_LENGTH_FULL)
                blockMessage.previousHash.substring(VBK_HASH_LENGTH).asVbkPreviousBlockHash()
            else
                blockMessage.previousHash.asVbkPreviousBlockHash(),

            if (blockMessage.secondPreviousHash.size() == VBK_HASH_LENGTH_FULL)
                blockMessage.secondPreviousHash.substring(VBK_HASH_LENGTH_FULL - VBK_KEYSTONE_HASH_LENGTH).asVbkPreviousKeystoneHash()
            else
                blockMessage.secondPreviousHash.asVbkPreviousKeystoneHash(),

            if (blockMessage.thirdPreviousHash.size() == VBK_HASH_LENGTH_FULL)
                blockMessage.thirdPreviousHash.substring(VBK_HASH_LENGTH_FULL - VBK_KEYSTONE_HASH_LENGTH).asVbkPreviousKeystoneHash()
            else
                blockMessage.thirdPreviousHash.asVbkPreviousKeystoneHash(),

            if (merkleRoot.length == VBK_HASH_LENGTH_FULL*2)
                merkleRoot.substring((VBK_HASH_LENGTH_FULL - TRUNCATED_MERKLE_ROOT_LENGTH)*2).asTruncatedMerkleRoot()
            else {
                logger.error { merkleRoot.length }
                merkleRoot.asTruncatedMerkleRoot()},

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

    private fun deserializeStandardTransaction(signedTransaction: RpcSignedTransaction): StandardTransaction {
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

        // TODO: add checks
        tx.publicKey = signedTransaction.publicKey.toByteArray()
        tx.signature = signedTransaction.signature.toByteArray()

        return tx
    }

    private fun deserializeMultisigTransaction(signedTransaction: RpcSignedMultisigTransaction): StandardTransaction {
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
    fun deserialize(raw: ByteArray?): RpcEvent? {
        try {
            return RpcEvent.parseFrom(raw)
        } catch (e: InvalidProtocolBufferException) {
            logger.error("Unable to parse message", e)
        }
        return null
    }
}

// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.serialization

import com.google.protobuf.InvalidProtocolBufferException
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.SignedMultisigTransaction
import nodecore.api.grpc.VeriBlockMessages.SignedTransaction
import nodecore.api.grpc.VeriBlockMessages.TransactionUnion
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.MerklePath
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import veriblock.model.BlockMetaPackage
import veriblock.model.FullBlock
import veriblock.model.MultisigTransaction
import veriblock.model.OutputFactory.create
import veriblock.model.PopTransactionLight
import veriblock.model.StandardTransaction
import veriblock.model.asLightAddress

private val logger = createLogger {}

object MessageSerializer {
    fun deserialize(blockHeaderMessage: VeriBlockMessages.BlockHeader): VeriBlockBlock {
        return SerializeDeserializeService.parseVeriBlockBlock(blockHeaderMessage.header.toByteArray())
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
        val tx = PopTransactionLight(Sha256Hash.wrap(txMessage.txId.toByteArray()))
        tx.inputAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress).asLightAddress()
        tx.endorsedBlock = SerializeDeserializeService.parseVeriBlockBlock(txMessage.endorsedBlockHeader.toByteArray())
        tx.bitcoinTx = BitcoinTransaction(txMessage.bitcoinTransaction.toByteArray())
        tx.bitcoinMerklePath = MerklePath(txMessage.merklePath)
        tx.blockOfProof = SerializeDeserializeService.parseBitcoinBlock(txMessage.bitcoinBlockHeaderOfProof.header.toByteArray())
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
            VBlakeHash.wrap(ByteStringUtility.byteStringToHex(blockMessage.previousHash)),
            VBlakeHash.wrap(ByteStringUtility.byteStringToHex(blockMessage.secondPreviousHash)),
            VBlakeHash.wrap(ByteStringUtility.byteStringToHex(blockMessage.thirdPreviousHash)),
            Sha256Hash.wrap(ByteStringUtility.byteStringToHex(blockMessage.merkleRoot)),
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
            Sha256Hash.wrap(blockMessage.blockContentMetapackage.hash.toByteArray())
        )
        return block
    }

    fun deserializeStandardTransaction(signedTransaction: SignedTransaction): StandardTransaction {
        val txMessage = signedTransaction.transaction
        val tx = StandardTransaction(Sha256Hash.wrap(txMessage.txId.toByteArray()))
        tx.inputAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress).asLightAddress()
        tx.inputAmount = Coin.valueOf(txMessage.sourceAmount)
        txMessage.outputsList.map {
            create(ByteStringAddressUtility.parseProperAddressTypeAutomatically(it.address), it.amount)
        }.forEach {
            tx.addOutput(it)
        }
        tx.setSignatureIndex(signedTransaction.signatureIndex)
        tx.data = txMessage.data.toByteArray()
        return tx
    }

    fun deserializeMultisigTransaction(signedTransaction: SignedMultisigTransaction): StandardTransaction {
        val txMessage = signedTransaction.transaction
        val tx: StandardTransaction = MultisigTransaction(Sha256Hash.wrap(txMessage.txId.toByteArray()))
        tx.inputAddress = ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress).asLightAddress()
        tx.inputAmount = Coin.valueOf(txMessage.sourceAmount)
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

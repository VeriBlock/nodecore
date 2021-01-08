// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.serialization

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.extensions.asVbkPreviousBlockHash
import nodecore.api.grpc.utilities.extensions.asVbkPreviousKeystoneHash
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.MerklePath
import org.veriblock.sdk.models.Output
import org.veriblock.core.crypto.asBtcHash
import org.veriblock.core.crypto.asTruncatedMerkleRoot
import org.veriblock.sdk.models.BlockMetaPackage
import org.veriblock.sdk.models.FullBlock
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPopTransaction
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.spv.model.StandardTransaction

private val logger = createLogger {}

fun VeriBlockMessages.BlockHeader.deserialize(): VeriBlockBlock =
    SerializeDeserializeService.parseVeriBlockBlock(header.toByteArray())

private fun VeriBlockMessages.TransactionUnion.deserializeNormalTransaction(transactionPrefix: Byte?): VeriBlockTransaction {
    return when (transactionCase) {
        VeriBlockMessages.TransactionUnion.TransactionCase.SIGNED -> signed.deserializeStandardTransaction(transactionPrefix)
        VeriBlockMessages.TransactionUnion.TransactionCase.SIGNED_MULTISIG -> signedMultisig.deserializeMultisigTransaction(transactionPrefix)
        else -> error("Can't deserialize a transaction with a transaction case: $transactionCase.") // Should be impossible
    }
}

fun VeriBlockMessages.TransactionUnion.deserializePoPTransaction(transactionPrefix: Byte?): VeriBlockPopTransaction =
    signed.deserializePoPTransaction(transactionPrefix)

fun VeriBlockMessages.Block.deserialize(transactionPrefix: Byte?): FullBlock {
    return FullBlock(
        number,
        version.toShort(),
        previousHash.asVbkPreviousBlockHash(),
        secondPreviousHash.asVbkPreviousKeystoneHash(),
        thirdPreviousHash.asVbkPreviousKeystoneHash(),
        ByteStringUtility.byteStringToHex(merkleRoot).asTruncatedMerkleRoot(),
        timestamp,
        encodedDifficulty,
        winningNonce,
        regularTransactionsList.map { tx -> tx.deserializeNormalTransaction(transactionPrefix) },
        popTransactionsList.map { tx -> tx.deserializePoPTransaction(transactionPrefix) },
        BlockMetaPackage(blockContentMetapackage.hash.toByteArray().asBtcHash())
    )
}

fun VeriBlockMessages.SignedTransaction.deserializePoPTransaction(transactionPrefix: Byte?): VeriBlockPopTransaction {
    val txMessage = transaction
    return VeriBlockPopTransaction(
        Address(ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress)),
        SerializeDeserializeService.parseVeriBlockBlock(txMessage.endorsedBlockHeader.toByteArray()),
        BitcoinTransaction(txMessage.bitcoinTransaction.toByteArray()),
        MerklePath(txMessage.merklePath),
        SerializeDeserializeService.parseBitcoinBlock(txMessage.bitcoinBlockHeaderOfProof.header.toByteArray()),
        txMessage.contextBitcoinBlockHeadersList.map { header ->
            SerializeDeserializeService.parseBitcoinBlock(header.header.toByteArray())
        },
        signature.toByteArray(),
        publicKey.toByteArray(),
        transactionPrefix
    )
}

fun StandardTransaction.deserializeStandardTransaction(transactionPrefix: Byte?): VeriBlockTransaction {
    return VeriBlockTransaction(
        1, // STANDARD
        Address(inputAddress!!.get()),
        inputAmount!!,
        getOutputs().map { o ->
            Output.of(o.address.get(), o.amount.atomicUnits)
        },
        getSignatureIndex(),
        SerializeDeserializeService.parsePublicationData(data!!),
        signature!!,
        publicKey!!,
        transactionPrefix
    )
}

fun VeriBlockMessages.SignedTransaction.deserializeStandardTransaction(transactionPrefix: Byte?): VeriBlockTransaction {
    val txMessage = transaction
    return VeriBlockTransaction(
        txMessage.type.number.toByte(),
        Address(ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress)),
        txMessage.sourceAmount.asCoin(),
        txMessage.outputsList.map { o ->
            Output.of(ByteStringAddressUtility.parseProperAddressTypeAutomatically(o.address), o.amount)
        },
        signatureIndex,
        SerializeDeserializeService.parsePublicationData(txMessage.data.toByteArray()),
        signature.toByteArray(),
        publicKey.toByteArray(),
        transactionPrefix
    )
}

fun VeriBlockMessages.SignedMultisigTransaction.deserializeMultisigTransaction(transactionPrefix: Byte?): VeriBlockTransaction {
    val txMessage = transaction
    return VeriBlockTransaction(
        txMessage.type.number.toByte(),
        Address(ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress)),
        txMessage.sourceAmount.asCoin(),
        txMessage.outputsList.map { o ->
            Output.of(ByteStringAddressUtility.parseProperAddressTypeAutomatically(o.address), o.amount)
        },
        signatureIndex,
        SerializeDeserializeService.parsePublicationData(txMessage.data.toByteArray()),
        ByteArray(0),
        ByteArray(0),
        transactionPrefix
    )
}

fun VeriBlockMessages.VeriBlockPublication.deserialize(transactionPrefix: Byte?): VeriBlockPublication {
    val context: List<VeriBlockBlock> =
        contextToEndorsedList.map {
            SerializeDeserializeService.parseVeriBlockBlock(it.toByteArray())
        } +
            SerializeDeserializeService.parseVeriBlockBlock(popTransaction.transaction.endorsedBlockHeader.toByteArray()) +
            contextToContainingList.map {
                SerializeDeserializeService.parseVeriBlockBlock(it.toByteArray())
            }

    return VeriBlockPublication(
        popTransaction.deserializePoPTransaction(transactionPrefix),
        VeriBlockMerklePath(compactMerklePath),
        SerializeDeserializeService.parseVeriBlockBlock(containingBlock.toByteArray()),
        context
    )
}

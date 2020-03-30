// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.serialization

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.core.BlockMetaPackage
import org.veriblock.lite.core.FullBlock
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.MerklePath
import org.veriblock.sdk.models.Output
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPoPTransaction
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.services.SerializeDeserializeService

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

fun VeriBlockMessages.TransactionUnion.deserializePoPTransaction(transactionPrefix: Byte?): VeriBlockPoPTransaction =
    signed.deserializePoPTransaction(transactionPrefix)

fun VeriBlockMessages.Block.deserialize(transactionPrefix: Byte?): FullBlock {
    return FullBlock(
        number,
        version.toShort(),
        VBlakeHash.wrap(ByteStringUtility.byteStringToHex(previousHash)),
        VBlakeHash.wrap(ByteStringUtility.byteStringToHex(secondPreviousHash)),
        VBlakeHash.wrap(ByteStringUtility.byteStringToHex(thirdPreviousHash)),
        Sha256Hash.wrap(ByteStringUtility.byteStringToHex(merkleRoot), 24),
        timestamp,
        encodedDifficulty,
        winningNonce,
        regularTransactionsList.map { tx -> tx.deserializeNormalTransaction(transactionPrefix) },
        popTransactionsList.map { tx -> tx.deserializePoPTransaction(transactionPrefix) },
        BlockMetaPackage(Sha256Hash.wrap(blockContentMetapackage.hash.toByteArray()))
    )
}

fun VeriBlockMessages.SignedTransaction.deserializePoPTransaction(transactionPrefix: Byte?): VeriBlockPoPTransaction {
    val txMessage = transaction
    return VeriBlockPoPTransaction(
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

fun VeriBlockMessages.SignedTransaction.deserializeStandardTransaction(transactionPrefix: Byte?): VeriBlockTransaction {
    val txMessage = transaction
    return VeriBlockTransaction(
        txMessage.type.number.toByte(),
        Address(ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.sourceAddress)),
        Coin.valueOf(txMessage.sourceAmount),
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
        Coin.valueOf(txMessage.sourceAmount),
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

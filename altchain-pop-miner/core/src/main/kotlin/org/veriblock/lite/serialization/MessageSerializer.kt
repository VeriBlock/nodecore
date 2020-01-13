// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
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
import org.veriblock.lite.params.NetworkParameters
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

fun VeriBlockMessages.BlockHeader.deserialize(params: NetworkParameters): VeriBlockBlock =
    SerializeDeserializeService.parseVeriBlockBlock(header.toByteArray())

private fun VeriBlockMessages.TransactionUnion.deserializeNormalTransaction(params: NetworkParameters): VeriBlockTransaction {
    return when (transactionCase) {
        VeriBlockMessages.TransactionUnion.TransactionCase.SIGNED -> signed.deserializeStandardTransaction(params)
        VeriBlockMessages.TransactionUnion.TransactionCase.SIGNED_MULTISIG -> signedMultisig.deserializeMultisigTransaction(params)
        else -> error("Can't deserialize a transaction with a transaction case: $transactionCase.") // Should be impossible
    }
}

fun VeriBlockMessages.TransactionUnion.deserializePoPTransaction(params: NetworkParameters): VeriBlockPoPTransaction =
    signed.deserializePoPTransaction(params)

fun VeriBlockMessages.Block.deserialize(params: NetworkParameters): FullBlock {
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
        regularTransactionsList.map { tx -> tx.deserializeNormalTransaction(params) },
        popTransactionsList.map { tx -> tx.deserializePoPTransaction(params) },
        BlockMetaPackage(Sha256Hash.wrap(blockContentMetapackage.hash.toByteArray()))
    )
}

fun VeriBlockMessages.SignedTransaction.deserializePoPTransaction(params: NetworkParameters): VeriBlockPoPTransaction {
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
        params.transactionPrefix
    )
}

fun VeriBlockMessages.SignedTransaction.deserializeStandardTransaction(params: NetworkParameters): VeriBlockTransaction {
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
        params.transactionPrefix
    )
}

fun VeriBlockMessages.SignedMultisigTransaction.deserializeMultisigTransaction(params: NetworkParameters): VeriBlockTransaction {
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
        params.transactionPrefix
    )
}

fun VeriBlockMessages.VeriBlockPublication.deserialize(params: NetworkParameters): VeriBlockPublication {
    val context: List<VeriBlockBlock> =
        contextToEndorsedList.map {
            SerializeDeserializeService.parseVeriBlockBlock(it.toByteArray())
        } +
        SerializeDeserializeService.parseVeriBlockBlock(popTransaction.transaction.endorsedBlockHeader.toByteArray()) +
        contextToContainingList.map {
            SerializeDeserializeService.parseVeriBlockBlock(it.toByteArray())
        }

    return VeriBlockPublication(
        popTransaction.deserializePoPTransaction(params),
        VeriBlockMerklePath(compactMerklePath),
        SerializeDeserializeService.parseVeriBlockBlock(containingBlock.toByteArray()),
        context
    )
}

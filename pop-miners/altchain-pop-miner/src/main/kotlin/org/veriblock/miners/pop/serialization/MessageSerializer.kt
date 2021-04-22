// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.serialization

import nodecore.api.grpc.RpcBlock
import nodecore.api.grpc.RpcBlockHeader
import nodecore.api.grpc.RpcSignedMultisigTransaction
import nodecore.api.grpc.RpcSignedTransaction
import nodecore.api.grpc.RpcTransactionUnion
import nodecore.api.grpc.RpcVeriBlockPublication
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

fun RpcBlockHeader.deserialize(): VeriBlockBlock =
    SerializeDeserializeService.parseVeriBlockBlock(header.toByteArray())

private fun RpcTransactionUnion.deserializeNormalTransaction(transactionPrefix: Byte?): VeriBlockTransaction {
    return when (transactionCase) {
        RpcTransactionUnion.TransactionCase.SIGNED -> signed.deserializeStandardTransaction(transactionPrefix)
        RpcTransactionUnion.TransactionCase.SIGNED_MULTISIG -> signedMultisig.deserializeMultisigTransaction(transactionPrefix)
        else -> error("Can't deserialize a transaction with a transaction case: $transactionCase.") // Should be impossible
    }
}

fun RpcTransactionUnion.deserializePoPTransaction(transactionPrefix: Byte?): VeriBlockPopTransaction =
    signed.deserializePoPTransaction(transactionPrefix)

fun RpcBlock.deserialize(transactionPrefix: Byte?): FullBlock {
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

fun RpcSignedTransaction.deserializePoPTransaction(transactionPrefix: Byte?): VeriBlockPopTransaction {
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

fun RpcSignedTransaction.deserializeStandardTransaction(transactionPrefix: Byte?): VeriBlockTransaction {
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

fun RpcSignedMultisigTransaction.deserializeMultisigTransaction(transactionPrefix: Byte?): VeriBlockTransaction {
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

fun RpcVeriBlockPublication.deserialize(transactionPrefix: Byte?): VeriBlockPublication {
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
